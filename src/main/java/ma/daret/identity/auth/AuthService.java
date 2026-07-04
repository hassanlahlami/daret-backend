package ma.daret.identity.auth;

import ma.daret.common.web.ApiException;
import ma.daret.identity.auth.AuthDtos.LoginRequest;
import ma.daret.identity.auth.AuthDtos.RegisterRequest;
import ma.daret.identity.auth.AuthDtos.TokenResponse;
import ma.daret.identity.kyc.KycProfile;
import ma.daret.identity.kyc.KycProfileRepository;
import ma.daret.identity.user.Role;
import ma.daret.identity.user.User;
import ma.daret.identity.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository users;
    private final KycProfileRepository kycProfiles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final LoginAttemptService loginAttempts;

    public AuthService(UserRepository users,
                       KycProfileRepository kycProfiles,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokens,
                       LoginAttemptService loginAttempts) {
        this.users = users;
        this.kycProfiles = kycProfiles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.loginAttempts = loginAttempts;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        String phone = normalize(req.phone());
        if (email == null && phone == null) {
            throw ApiException.badRequest("CONTACT_REQUIRED", "Provide an email or a phone number");
        }
        if (email != null && users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email already registered");
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw ApiException.conflict("PHONE_TAKEN", "Phone already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.addRole(Role.MEMBER);
        users.save(user);

        // Every member starts with a PENDING KYC profile.
        kycProfiles.save(new KycProfile(user.getId()));

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        String email = normalize(req.email());
        String phone = normalize(req.phone());
        String key = email != null ? email : phone;

        if (key != null && loginAttempts.isLocked(key)) {
            throw new ApiException(org.springframework.http.HttpStatus.LOCKED,
                    "ACCOUNT_LOCKED", "Too many failed attempts — try again later");
        }

        User user = (email != null ? users.findByEmail(email) : users.findByPhone(phone)).orElse(null);
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            if (key != null) {
                loginAttempts.recordFailure(key);
            }
            throw ApiException.unauthorized("BAD_CREDENTIALS", "Invalid credentials");
        }
        if (key != null) {
            loginAttempts.reset(key);
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawRefreshToken);
        User user = users.findById(rotation.userId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH", "User no longer exists"));
        return TokenResponse.bearer(jwtService.issueAccessToken(user), rotation.rawToken());
    }

    private TokenResponse issueTokens(User user) {
        return TokenResponse.bearer(jwtService.issueAccessToken(user), refreshTokens.issue(user));
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }
}
