第一天（慎重看解释，正确性待商榷，冲突以纠正栏为准）：
login：
当前为开发环境，所以springboot会读取application-dev覆盖application.yml，
jwt被设置为120分钟，服务器被配置成18080端口，上下文路径为/api，所以之后的url打头都有/api然后接路径。
用户登入的时候，会调用AuthController中的login方法，但在此之前，@RequiredArgsConstructor会给public class AuthController进行依赖注入userService，然后@Validated会对其进行参数检验检测是否注入（为null也可以），然后正式调用 public Result<String> login(@Valid @RequestBody UserLoginRequest request)，@RequestBody 将前端传来的json数据封装成UserLoginRequest，@Valid检测是否有传入了UserLoginRequest类型参数（即便是null，也可以），
controller调用userService.login(request)方法，先进入UserService接口，然后调用它的实现类public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService， @Service将他纳入spring环境中（虽然不知道有什么用），@RequiredArgsConstructor来给    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;进行依赖注入，此后，调用    public String login(UserLoginRequest request) 其中User user = lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .one();进行mybitsplus的查询语句，查询数据库中的用户username是否有和request参数的username相同的数据存在，用User类型封装这唯一的参数（或者是null），
然后进行检测，如果是null便是不存在用户，然后检查他的status，先检查是不是null免得出bug，但是我个人认为就不应该出现是null的逻辑，不知道为什么要检验它，不是null情况下，检查是不是0，0为被禁用了，便调用自定义异常BusinessException("账号已被禁用，请联系管理员")，然后检查密码对不对，        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }，对于我个人，我因为看不懂这个库函数，如果我来实现，我不会写这个库函数，我会老老实实用密码看是不是相等的。最后封装jwt，调用jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());，返回给controll层jwt，controll会调用result封装， public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }，让我想不到jwt是一个字符串，利用泛型封装了String，token被封装到data里，之后将其返回给前端。


纠正：
1.@RequiredArgsConstructor只负责生成构造函数，Lombok 会生成：

public AuthController(UserService userService){
    this.userService = userService;
}，Spring IOC 容器调用这个构造函数进行依赖注入，Lombok负责生成构造方法，Spring负责注入。

2. @Validated 不是检测依赖注入的，而是开启方法参数校验功能。例如在 Controller 类上添加 @Validated 后，方法参数上的 @Min、@NotBlank 等校验注解才会生效。

3. @Valid 的作用是校验 DTO 对象内部字段是否合法，例如 LoginDTO 中的 @NotBlank。@Validated 和 @Valid 都可以触发 Bean Validation，但 @Validated 常用于方法参数校验，而 @Valid 常用于对象校验。

4.if(user.getStatus() != null && user.getStatus() == 0) 原因是：防御式编程
防止：旧数据 据库异常 迁移问题

5.@Service作用是：注册为Spring Bean
Spring启动时会扫描：
@Service
@Component
@Controller
然后：
加入IOC容器
这样才能：
自动注入
否则：
@Autowired
@RequiredArgsConstructor
都会失效。

6.BCrypt 是：密码哈希算法
特点：
1️⃣ 自动加 salt
2️⃣ 每次加密结果都不同
3️⃣ 计算慢（防止暴力破解）
校验代码：passwordEncoder.matches(明文密码, 数据库密码)

7.notlank不能为 null
不能为 ""
不能只包含空格




改进方向：
1.编码问题，对jwt过期时间做了硬编码，之后复刻项目，做一个@ConfigurationProperties，建造一个配置类，如@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private long expireMinutes;

    // getter setter
}然后@RequiredArgsConstructor
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

}









































