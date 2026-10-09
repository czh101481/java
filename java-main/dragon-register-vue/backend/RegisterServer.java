import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 龙渊·注册后端（零依赖，JDK 17+ 直接运行）
 *
 *   启动:  java RegisterServer.java        （在本文件所在目录执行）
 *   或双击: start.bat
 *
 * 接口契约（与前端 src/register/api/auth.js 严格对齐）:
 *   POST /api/register  {username, email, password}
 *     → {success:boolean, message:string, field?:'username'|'email'|'password'}
 *   POST /api/login     {username|email, password}
 *     → {success:boolean, message:string}
 *   GET  /api/health    → {success:true, message:'ok'}
 *
 * 数据落盘: data/users.json（密码为 PBKDF2WithHmacSHA256 加盐哈希，绝不存明文）
 */
public class RegisterServer {

    static final int PORT = 8081;
    static final Path DATA_FILE = Path.of("data", "users.json");

    /** 与前端密码强度档位一致：最低 8 位 */
    static final int MIN_PWD_LEN = 8;
    /** 与前端 EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/ 完全一致 */
    static final String EMAIL_RE = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    static final List<User> USERS = Collections.synchronizedList(new ArrayList<>());
    static final SecureRandom RNG = new SecureRandom();

    public static void main(String[] args) throws IOException {
        loadUsers();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/api/register", RegisterServer::handleRegister);
        server.createContext("/api/login", RegisterServer::handleLogin);
        server.createContext("/api/health", ex -> respond(ex, 200, Map.of("success", true, "message", "ok")));
        server.start();
        System.out.println("[dragon-auth] http://localhost:" + PORT + "  (users: " + USERS.size() + ")");
        System.out.println("[dragon-auth] data file: " + DATA_FILE.toAbsolutePath());
    }

    // ---------------------------------------------------------------- 注册

    static void handleRegister(HttpExchange ex) throws IOException {
        try {
            if (preflight(ex)) return;

            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                respond(ex, 405, Map.of("success", false, "message", "只接受 POST"));
                return;
            }

            Map<String, String> body = parseJsonObject(readBody(ex));
            String username = trimToEmpty(body.get("username"));
            String email = trimToEmpty(body.get("email")).toLowerCase();
            String password = body.get("password") == null ? "" : body.get("password");

            // ---- 校验（规则与前端 onSubmit 完全对齐）----
            if (username.length() < 3 || username.length() > 20) {
                fail(ex, 400, "名号需 3–20 个字符", "username");
                return;
            }
            if (!email.matches(EMAIL_RE)) {
                fail(ex, 400, "请输入有效的邮箱地址", "email");
                return;
            }
            if (password.length() < MIN_PWD_LEN) {
                fail(ex, 400, "秘钥至少 " + MIN_PWD_LEN + " 位，建议混合字符", "password");
                return;
            }

            // ---- 查重（用户名/邮箱均忽略大小写）----
            synchronized (USERS) {
                for (User u : USERS) {
                    if (u.username().equalsIgnoreCase(username)) {
                        fail(ex, 409, "此名号已被铭刻，请另择一名", "username");
                        return;
                    }
                    if (u.email().equalsIgnoreCase(email)) {
                        fail(ex, 409, "此邮箱已缔结契约，请直接登录", "email");
                        return;
                    }
                }
                USERS.add(User.create(username, email, password));
                saveUsers();
            }

            respond(ex, 200, Map.of(
                    "success", true,
                    "message", "觉醒成功，正在前往龙渊登录…"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            respond(ex, 500, Map.of("success", false, "message", "龙渊灵脉紊乱，请稍后再试"));
        }
    }

    // ---------------------------------------------------------------- 登录

    static void handleLogin(HttpExchange ex) throws IOException {
        try {
            if (preflight(ex)) return;

            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                respond(ex, 405, Map.of("success", false, "message", "只接受 POST"));
                return;
            }

            Map<String, String> body = parseJsonObject(readBody(ex));
            String account = trimToEmpty(body.getOrDefault("username",
                    body.get("email") != null ? body.get("email") : "")).toLowerCase();
            String password = body.get("password") == null ? "" : body.get("password");

            if (account.isEmpty() || password.isEmpty()) {
                respond(ex, 400, Map.of("success", false, "message", "请填写名号与秘钥"));
                return;
            }

            User hit = null;
            synchronized (USERS) {
                for (User u : USERS) {
                    if (u.username().equalsIgnoreCase(account) || u.email().equalsIgnoreCase(account)) {
                        hit = u; break;
                    }
                }
            }
            if (hit != null && hit.matches(password)) {
                respond(ex, 200, Map.of("success", true, "message", "契约验证通过，欢迎回到龙渊"));
            } else {
                respond(ex, 401, Map.of("success", false, "message", "名号或秘钥不正确"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            respond(ex, 500, Map.of("success", false, "message", "龙渊灵脉紊乱，请稍后再试"));
        }
    }

    // ---------------------------------------------------------------- 数据

    record User(String username, String email, String saltHex, String hashHex, String createdAt) {

        static User create(String username, String email, String password) {
            byte[] salt = new byte[16];
            RNG.nextBytes(salt);
            byte[] hash = pbkdf2(password, salt);
            return new User(username, email, hex(salt), hex(hash), Instant.now().toString());
        }

        boolean matches(String password) {
            byte[] salt = unhex(saltHex());
            return MessageDigest.isEqual(pbkdf2(password, salt), unhex(hashHex()));
        }
    }

    static byte[] pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 不可用", e);
        }
    }

    static void loadUsers() {
        try {
            if (Files.exists(DATA_FILE)) {
                String json = Files.readString(DATA_FILE, StandardCharsets.UTF_8);
                for (String obj : splitTopLevelObjects(json)) {
                    Map<String, String> m = parseJsonObject(obj);
                    if (m.containsKey("username") && m.containsKey("email")) {
                        USERS.add(new User(m.get("username"), m.get("email"),
                                m.get("salt"), m.get("hash"), m.get("createdAt")));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[dragon-auth] 读取数据文件失败，按空库启动: " + e.getMessage());
        }
    }

    static void saveUsers() throws IOException {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < USERS.size(); i++) {
            User u = USERS.get(i);
            sb.append("  {\"username\":\"").append(jsonEscape(u.username()))
              .append("\",\"email\":\"").append(jsonEscape(u.email()))
              .append("\",\"salt\":\"").append(u.saltHex())
              .append("\",\"hash\":\"").append(u.hashHex())
              .append("\",\"createdAt\":\"").append(u.createdAt())
              .append("\"}").append(i < USERS.size() - 1 ? "," : "").append("\n");
        }
        sb.append("]\n");
        Files.createDirectories(DATA_FILE.getParent());
        Files.writeString(DATA_FILE, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    // ---------------------------------------------------------------- HTTP 工具

    /** CORS 预检：开发期前端跑在 5173/4173，直接放行 */
    static boolean preflight(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return true;
        }
        return false;
    }

    static void cors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    static void fail(HttpExchange ex, int status, String message, String field) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", message);
        m.put("field", field);
        respond(ex, status, m);
    }

    static void respond(HttpExchange ex, int status, Map<String, Object> body) throws IOException {
        cors(ex);
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : body.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof Boolean || v instanceof Number) sb.append(v);
            else sb.append("\"").append(jsonEscape(String.valueOf(v))).append("\"");
        }
        sb.append("}");
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    // ---------------------------------------------------------------- 迷你 JSON（仅解析扁平字符串对象，零依赖）

    /** 解析形如 {"k":"v",...} 的扁平 JSON 对象；解析失败返回空 Map（由上层按缺参处理） */
    static Map<String, String> parseJsonObject(String s) {
        Map<String, String> out = new LinkedHashMap<>();
        if (s == null) return out;
        int i = skipWs(s, 0);
        if (i >= s.length() || s.charAt(i) != '{') return out;
        i++;
        int[] pos = new int[1];
        while (true) {
            i = skipWs(s, i);
            if (i >= s.length()) break;
            char c = s.charAt(i);
            if (c == '}') break;
            if (c == ',') { i++; continue; }
            if (c != '"') break;
            String key = parseString(s, i, pos);
            i = skipWs(s, pos[0]);
            if (i >= s.length() || s.charAt(i) != ':') break;
            i = skipWs(s, i + 1);
            if (i >= s.length()) break;
            if (s.charAt(i) == '"') {
                out.put(key, parseString(s, i, pos));
                i = pos[0];
            } else {
                while (i < s.length() && s.charAt(i) != ',' && s.charAt(i) != '}') i++;
            }
        }
        return out;
    }

    /** 解析从 start 开始的 JSON 字符串，返回内容并把结束位置写入 pos[0] */
    static String parseString(String s, int start, int[] pos) {
        StringBuilder sb = new StringBuilder();
        int i = start + 1; // 跳过开头引号
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') { pos[0] = i + 1; return sb.toString(); }
            if (c == '\\' && i + 1 < s.length()) {
                char e = s.charAt(++i);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 < s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                    }
                    default -> sb.append(e);
                }
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        pos[0] = i;
        return sb.toString();
    }

    /** 按顶层大括号切分数组元素（字符串内的括号不计） */
    static List<String> splitTopLevelObjects(String json) {
        List<String> parts = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inStr = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inStr) {
                if (c == '\\') i++;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') inStr = true;
            else if (c == '{') { if (depth++ == 0) start = i; }
            else if (c == '}') { if (--depth == 0 && start >= 0) parts.add(json.substring(start, i + 1)); }
        }
        return parts;
    }

    static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static byte[] unhex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
