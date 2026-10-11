package main

import (
	"encoding/binary"
	"log"
	"net"
	"net/http"
	"os"
	"strconv"
	"strings"
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"
)

var (
	listenHost     = env("VOICE_RELAY_HOST", "0.0.0.0")
	listenPort     = env("VOICE_RELAY_PORT", "8790")
	defaultHost    = env("VOICE_UPSTREAM_HOST", "mc.euphorium.ru")
	allowPorts     = parsePorts(env("VOICE_ALLOW_PORTS", ""))
	allowHosts     = parseHosts(env("VOICE_ALLOWED_HOSTS", ""))
	maxClients     = envInt("VOICE_MAX_CLIENTS", 512)
	idleTimeout    = time.Duration(envInt("VOICE_IDLE_TIMEOUT", 120)) * time.Second
	helloTimeout   = time.Duration(envInt("VOICE_HELLO_TIMEOUT", 5)) * time.Second
	maxFrame       = int64(envInt("VOICE_MAX_FRAME", 256*1024))
	allowPrivate   = env("VOICE_ALLOW_PRIVATE", "false") == "true"
)

var (
	pvMagic = []byte{0x4E, 0x90, 0x04, 0xE9}
)

var (
	nClients int64
	nTotal   int64
	nC2S     int64
	nS2C     int64
	nDropped int64
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  4096,
	WriteBufferSize: 4096,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

func env(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return d
}

func envInt(k string, d int) int {
	if v := os.Getenv(k); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}
	return d
}

func parsePorts(s string) map[int]bool {
	m := map[int]bool{}
	for _, p := range strings.Split(strings.ReplaceAll(s, " ", ""), ",") {
		if p == "" {
			continue
		}
		if n, err := strconv.Atoi(p); err == nil {
			m[n] = true
		}
	}
	return m
}

func parseHosts(s string) map[string]bool {
	m := map[string]bool{}
	for _, h := range strings.Split(s, ",") {
		h = strings.ToLower(strings.TrimSpace(h))
		if h != "" {
			m[h] = true
		}
	}
	return m
}

func portAllowed(port int) bool {
	if len(allowPorts) == 0 {
		return port >= 1024 && port <= 65535
	}
	return allowPorts[port]
}

func isPublicIP(ip net.IP) bool {
	return !(ip.IsLoopback() || ip.IsPrivate() || ip.IsLinkLocalUnicast() ||
		ip.IsLinkLocalMulticast() || ip.IsUnspecified() || ip.IsMulticast())
}

// resolveTarget turns a host (name or IP) into a dialable IP, refusing private addresses.
func resolveTarget(host string) (string, bool) {
	if ip := net.ParseIP(host); ip != nil {
		if !allowPrivate && !isPublicIP(ip) {
			return "", false
		}
		return ip.String(), true
	}

	ips, err := net.LookupIP(host)
	if err != nil || len(ips) == 0 {
		return "", false
	}
	for _, ip := range ips {
		if allowPrivate || isPublicIP(ip) {
			return ip.String(), true
		}
	}
	return "", false
}

func hostAllowed(host string, ip string) bool {
	if len(allowHosts) == 0 {
		return true
	}
	if allowHosts[strings.ToLower(host)] {
		return true
	}
	// allow if the resolved IP matches one of the allowlisted hosts' IPs
	for allowed := range allowHosts {
		ips, err := net.LookupIP(allowed)
		if err != nil {
			continue
		}
		for _, a := range ips {
			if a.String() == ip {
				return true
			}
		}
	}
	return false
}

type conn struct {
	ws       *websocket.Conn
	udp      *net.UDPConn
	cid      int64
	lastSeen atomic.Int64
	closed   chan struct{}
}

func (c *conn) touch() { c.lastSeen.Store(time.Now().UnixNano()) }

func (c *conn) clientToUpstream() {
	for {
		mt, data, err := c.ws.ReadMessage()
		if err != nil {
			return
		}
		if mt != websocket.BinaryMessage || len(data) < 4 || string(data[:4]) != string(pvMagic) {
			atomic.AddInt64(&nDropped, 1)
			continue
		}
		if _, err := c.udp.Write(data); err != nil {
			return
		}
		atomic.AddInt64(&nC2S, 1)
		c.touch()
	}
}

func (c *conn) upstreamToClient() {
	buf := make([]byte, 65535)
	for {
		n, err := c.udp.Read(buf)
		if err != nil {
			return
		}
		if err := c.ws.WriteMessage(websocket.BinaryMessage, buf[:n]); err != nil {
			return
		}
		atomic.AddInt64(&nS2C, 1)
		c.touch()
	}
}

func (c *conn) watchdog() {
	t := time.NewTicker(5 * time.Second)
	defer t.Stop()
	for {
		select {
		case <-c.closed:
			return
		case <-t.C:
			if time.Since(time.Unix(0, c.lastSeen.Load())) > idleTimeout {
				log.Printf("#%d idle timeout", c.cid)
				c.ws.Close()
				return
			}
		}
	}
}

// parseHello returns (upstreamHost, port, ok).
func parseHello(hello []byte) (string, int, bool) {
	if len(hello) < 7 {
		return "", 0, false
	}
	switch string(hello[:4]) {
	case "PVW1":
		if hello[4] != 1 {
			return "", 0, false
		}
		return "", int(binary.BigEndian.Uint16(hello[5:7])), true
	case "PVW2":
		if hello[4] != 2 || len(hello) < 8 {
			return "", 0, false
		}
		port := int(binary.BigEndian.Uint16(hello[5:7]))
		ipLen := int(hello[7])
		if len(hello) < 8+ipLen {
			return "", 0, false
		}
		return string(hello[8 : 8+ipLen]), port, true
	default:
		return "", 0, false
	}
}

func handle(w http.ResponseWriter, r *http.Request) {
	if atomic.LoadInt64(&nClients) >= int64(maxClients) {
		http.Error(w, "busy", http.StatusServiceUnavailable)
		return
	}
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		return
	}
	defer ws.Close()
	ws.SetReadLimit(maxFrame)

	ws.SetReadDeadline(time.Now().Add(helloTimeout))
	mt, hello, err := ws.ReadMessage()
	if err != nil || mt != websocket.BinaryMessage {
		return
	}

	clientHost, upstreamPort, ok := parseHello(hello)
	if !ok {
		log.Printf("reject %s: bad hello", r.RemoteAddr)
		ws.WriteControl(websocket.CloseMessage,
			websocket.FormatCloseMessage(websocket.ClosePolicyViolation, "bad hello"),
			time.Now().Add(time.Second))
		return
	}
	if !portAllowed(upstreamPort) {
		log.Printf("reject %s: port %d not allowed", r.RemoteAddr, upstreamPort)
		ws.WriteControl(websocket.CloseMessage,
			websocket.FormatCloseMessage(websocket.ClosePolicyViolation, "port not allowed"),
			time.Now().Add(time.Second))
		return
	}

	host := clientHost
	if host == "" {
		host = defaultHost
	}
	targetIP, pub := resolveTarget(host)
	if !pub || !hostAllowed(host, targetIP) {
		log.Printf("reject %s: upstream %q not allowed", r.RemoteAddr, host)
		ws.WriteControl(websocket.CloseMessage,
			websocket.FormatCloseMessage(websocket.ClosePolicyViolation, "upstream not allowed"),
			time.Now().Add(time.Second))
		return
	}

	udp, err := net.Dial("udp", net.JoinHostPort(targetIP, strconv.Itoa(upstreamPort)))
	if err != nil {
		log.Printf("upstream dial failed: %v", err)
		return
	}
	defer udp.Close()

	c := &conn{ws: ws, udp: udp.(*net.UDPConn), cid: atomic.AddInt64(&nTotal, 1), closed: make(chan struct{})}
	atomic.AddInt64(&nClients, 1)
	defer atomic.AddInt64(&nClients, -1)
	c.touch()

	ws.SetReadDeadline(time.Time{})
	log.Printf("#%d %s -> %s (%s):%d  (clients=%d)", c.cid, r.RemoteAddr, host, targetIP, upstreamPort, atomic.LoadInt64(&nClients))

	go c.upstreamToClient()
	go c.watchdog()

	c.clientToUpstream()
	close(c.closed)
}

func reporter() {
	for {
		time.Sleep(60 * time.Second)
		log.Printf("stats clients=%d total=%d c2s=%d s2c=%d dropped=%d",
			atomic.LoadInt64(&nClients), atomic.LoadInt64(&nTotal),
			atomic.LoadInt64(&nC2S), atomic.LoadInt64(&nS2C), atomic.LoadInt64(&nDropped))
	}
}

func main() {
	log.SetOutput(os.Stdout)
	log.SetFlags(log.LstdFlags)
	ports := "1024-65535"
	if len(allowPorts) > 0 {
		ports = "whitelist"
	}
	hosts := "any-public"
	if len(allowHosts) > 0 {
		hosts = "whitelist"
	}
	log.Printf("voice-relay listening on %s:%s, default_upstream=%s, ports=%s, hosts=%s", listenHost, listenPort, defaultHost, ports, hosts)
	go reporter()
	srv := &http.Server{Addr: net.JoinHostPort(listenHost, listenPort), Handler: http.HandlerFunc(handle)}
	if err := srv.ListenAndServe(); err != nil {
		log.Fatal(err)
	}
}
