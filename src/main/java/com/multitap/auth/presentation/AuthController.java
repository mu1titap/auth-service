package com.multitap.auth.presentation;

import com.multitap.auth.application.AuthService;
import com.multitap.auth.application.BlackListService;
import com.multitap.auth.application.EmailService;
import com.multitap.auth.common.jwt.JwtTokenProvider;
import com.multitap.auth.common.response.BaseResponse;
import com.multitap.auth.dto.in.*;
import com.multitap.auth.vo.in.*;
import com.multitap.auth.vo.out.SignInResponseVo;
import com.multitap.auth.vo.out.UuidResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "계정 관리 API", description = "계정 관련 API endpoints")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final BlackListService blackListService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "회원가입", description = "회원가입 기능입니다.")
    @PostMapping("/sign-up")
    public BaseResponse<UuidResponseVo> signUp(@RequestBody SignUpRequestVo signUpRequestVo) {
        return new BaseResponse<>(authService.signUp(SignUpRequestDto.from(signUpRequestVo)).toVo());
    }

    @Operation(summary = "로그인", description = "로그인 기능입니다.")
    @PostMapping("/sign-in")
    public BaseResponse<SignInResponseVo> signIn(@RequestBody SignInRequestVo signInRequestVo) {
        return new BaseResponse<>(authService.signIn(SignInRequestDto.from(signInRequestVo)).toVo());
    }

    @Operation(summary = "소셜 로그인", description = "소셜 로그인 기능입니다.")
    @PostMapping("/oauth-sign-in")
    public BaseResponse<SignInResponseVo> oAuthSignIn(@RequestBody OAuthSignInRequestVo oAuthSignInRequestVo) {
        return new BaseResponse<>(authService.oAuthSignIn(OAuthSignInRequestDto.from(oAuthSignInRequestVo)).toVo());
    }

    @Operation(summary = "로그아웃", description = "로그아웃 기능입니다.")
    @PostMapping("/sign-out")
    public BaseResponse<Void> signOut(@RequestHeader("Authorization") String token) {
        log.info("token: {}", token);
        String jwtToken = token.substring(7);
        long expiration = jwtTokenProvider.getExpiration(jwtToken);
        blackListService.addToBlacklist(jwtToken, expiration);
        return new BaseResponse<>();
    }

    @Operation(summary = "아이디 찾기", description = "이메일 인증을 통해 아이디를 찾습니다.")
    @PostMapping("/find-id")
    public BaseResponse<Void> findId(@RequestBody FindIdRequestVo findIdRequestVo) {
        emailService.sendAccountIdEmail(FindIdRequestDto.from(findIdRequestVo));
        return new BaseResponse<>();
    }

    @Operation(summary = "비밀번호 찾기", description = "이메일 인증을 통해 임시 비밀번호를 발급 받습니다.")
    @PostMapping("/reset-password")
    public BaseResponse<Void> resetPassword(@RequestBody FindPasswordRequestVo findPasswordRequestVo) {
        emailService.sendTemporaryPasswordEmail(FindPasswordRequestDto.from(findPasswordRequestVo));
        return new BaseResponse<>();
    }


    @Operation(summary = "비밀번호 재설정", description = "비밀번호를 재설정합니다.")
    @PostMapping("/change-password")
    public BaseResponse<Void> changePassword(@RequestBody NewPasswordRequestVo newPasswordRequestVo, @RequestHeader("Authorization") String token, @RequestHeader("Uuid") String uuid) {
        authService.changePassword(NewPasswordRequestDto.from(newPasswordRequestVo, uuid));
        String jwtToken = token.substring(7);
        long expiration = jwtTokenProvider.getExpiration(jwtToken);
        blackListService.addToBlacklist(jwtToken, expiration);
        return new BaseResponse<>();
    }

    @Operation(summary = "회원 정보 수정", description = "닉네임 또는 전화번호를 수정합니다.")
    @PutMapping("/change-memberInfo")
    public BaseResponse<Void> changeMemberInfo(@RequestBody MemberInfoRequestVo memberInfoRequestVo, @RequestHeader("Uuid") String uuid) {
        authService.changeMemberInfo(MemberInfoRequestDto.from(memberInfoRequestVo, uuid));
        return new BaseResponse<>();
    }

}
