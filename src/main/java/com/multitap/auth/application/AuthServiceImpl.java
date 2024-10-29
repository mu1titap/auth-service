package com.multitap.auth.application;

import com.multitap.auth.dto.in.*;
import com.multitap.auth.dto.out.FindIdResponseDto;
import com.multitap.auth.dto.out.SignInResponseDto;
import com.multitap.auth.entity.AuthUserDetail;
import com.multitap.auth.entity.Member;
import com.multitap.auth.infrastructure.MemberRepository;
import com.multitap.auth.infrastructure.OAuthRepository;
import com.multitap.auth.common.exception.BaseException;
import com.multitap.auth.common.jwt.JwtTokenProvider;
import com.multitap.auth.common.response.BaseResponseStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final OAuthRepository oAuthRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void signUp(SignUpRequestDto signUpRequestDto) {

        if (memberRepository.findByEmail(signUpRequestDto.getEmail()).isPresent()) {
            throw new BaseException(BaseResponseStatus.DUPLICATED_USER);
        }
        memberRepository.save(signUpRequestDto.toEntity(passwordEncoder));
    }

    @Override
    public SignInResponseDto signIn(SignInRequestDto signInRequestDto) {

        Member member = memberRepository.findByAccountId(signInRequestDto.getAccountId()).orElseThrow(
                () -> new BaseException(BaseResponseStatus.FAILED_TO_LOGIN));

        if (!new BCryptPasswordEncoder().matches(signInRequestDto.getPassword(), member.getPassword())) {
            throw new BaseException(BaseResponseStatus.FAILED_TO_LOGIN);
        }

        return createToken(authenticate(member, signInRequestDto.getPassword()));
    }

    @Override
    public SignInResponseDto oAuthSignIn(OAuthSignInRequestDto oAuthSignInRequestDto) {

        Member member = memberRepository.findByEmail(oAuthSignInRequestDto.getEmail()).orElseThrow(
                () -> new BaseException(BaseResponseStatus.NO_EXIST_USER) // 회원가입 페이지로 이동
        );

        oAuthRepository.findByMemberUuid(member.getUuid()).orElseGet(
                () -> oAuthRepository.save(oAuthSignInRequestDto.toEntity(member.getUuid()))
        );

        return createToken(oAuthAuthenticate(member));
    }

    //todo: 로그아웃 시  토큰 블랙리스트 등록, 비밀번호 재설정 시 토큰 블랙리스트 등록, 아이디 찾기 구현 +) 회원 탈퇴 구현
    @Override
    public void signOut() {

    }

    @Override
    public FindIdResponseDto findId(FindIdRequestDto findIdRequestDto) {
        Member member = memberRepository.findByEmail(findIdRequestDto.getEmail()).orElseThrow(
                () -> new BaseException(BaseResponseStatus.INVALID_EMAIL_ADDRESS)
        );
        return FindIdResponseDto.from(member);
    }


    @Override
    public void changePassword(PasswordChangeRequestDto passwordChangeRequestDto) {
        Member member = memberRepository.findByUuid(passwordChangeRequestDto.getUuid()).orElseThrow(
                () -> new BaseException(BaseResponseStatus.NO_EXIST_USER)
        );
        memberRepository.save(passwordChangeRequestDto.toEntity(passwordChangeRequestDto, member, passwordEncoder));
    }


    private SignInResponseDto createToken(Authentication authentication) {
        AuthUserDetail authUserDetail = (AuthUserDetail) authentication.getPrincipal();
        return jwtTokenProvider.generateToken(authUserDetail);
    }

    private Authentication authenticate(Member member, String inputPassword) {
        AuthUserDetail authUserDetail = new AuthUserDetail(member);
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authUserDetail.getAccountId(),
                        inputPassword
                )
        );
    }

    private Authentication oAuthAuthenticate(Member member) {
        AuthUserDetail authUserDetail = new AuthUserDetail(member);
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authUserDetail.getEmail(),
                        null
                )
        );
    }

}