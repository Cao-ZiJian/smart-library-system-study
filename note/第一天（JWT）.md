第一天（慎重看解释，正确性待商榷，冲突以纠正栏为准）
JwtUtils:
@Component注解让其可以被spring扫描到，进项依赖注入，并把public class JwtUtil即本类注册为bean，貌似只有这点功能。
配置文件注入secret和expireMinutes。
 public String generateToken(Long userId, String username, String role) 
Date now = new Date();给now赋值milliseconds since January 1, 1970, 00:00:00 GMT.
now.getTime()不明白是干什么的，
Date expireDate = new Date(now.getTime() + expireMinutes * 60 * 1000);赋值过期时间，具体是什么不清楚
return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
payload中的sub赋值userId，然后赋值"username"：username和"role"： role
setIssuedAt(now)赋milliseconds since January 1, 1970, 00:00:00 GMT.
.setExpiration(expireDate)设置过期时间
 .signWith(SignatureAlgorithm.HS256, secret)使用HMAC-SHA256 签名算法，并且密钥注入
.compact()这个应该是生成jwt的意思

public Claims parseToken(String token)解析jwt
Jwts.parser() 使用Jwt库的parser开始解析
.setSigningKey(secret)输入密钥
.parseClaimsJws(token)注入token开始解析，并返回关于claim的Map集合
.getBody大概只有提示完成了的意思


纠正：
1.@Component 的作用：
1 让 Spring 扫描到这个类
2 创建对象
3 放入 IOC 容器


2.now.getTime()
意思是：
获取当前时间的时间戳（毫秒）

3.假设生成 token 时：
.setSubject("1")
.claim("username", "admin")
.claim("role", "ADMIN")
那么解析后 Claims 里面的数据就是：
{
  sub: "1",
  username: "admin",
  role: "ADMIN",
  iat: 1710000000,
  exp: 1710003600
}

4.Date expireDate = new Date(now.getTime() + expireMinutes * 60 * 1000);
建议补一句解释，不然以后回看可能忘：
expireMinutes * 60 * 1000
把分钟转换成毫秒
整句意思：
当前时间 + 有效期 = token过期时间

5.实际上 JWT payload 可以放数字。
更准确说法：
subject 是字符串类型，所以通常转成 String

6.getBody()
作用：
获取解析后的 JWT payload 数据
返回类型：Claims

















































