package com.y.java_board.controller;

import com.y.java_board.config.UserInfoSession;
import com.y.java_board.domain.User;
import com.y.java_board.dto.UserDto;
import com.y.java_board.service.ImageService;
import com.y.java_board.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Controller
@RequiredArgsConstructor
@SessionAttributes("userInfo")
public class UserController {

    private final UserService userService;
    private final ImageService imageService;
    private final PasswordEncoder passwordEncoder;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping("/user/register")
    public String showSignUpForm(ModelMap model) {
        model.addAttribute("userDto", new User());
        return "user/signup";
    }

    @PostMapping("/user/register")
    public String signUp(UserDto userDto) {
        try {
            User user = userDto.toEntity();
            user.setPassword(passwordEncoder.encode(userDto.password()));

            userService.register(user);
        } catch (IllegalStateException e) {
            // TODO : 오류 처리
        }
        return "redirect:/";
    }

    @GetMapping("/user/info")
    public String showUserInfo(ModelMap model, @ModelAttribute("userInfo") UserInfoSession userInfoSession) {
        String downloadDirectory = "src/main/resources/static/images/profile";
        try {
            byte[] profileImageBytes = imageService.getImage(downloadDirectory, userInfoSession.getProfileImage()); // Unhandled exception : java.io.IOException
            String profileImageBase64 = Base64.getEncoder().encodeToString(profileImageBytes);
            model.put("profileImage", profileImageBase64);
        } catch (IOException e) {
            model.put("profileImage", "");
        }
        return "user/info";
    }

    @GetMapping("/user/profile")
    public String showUserProfileForm(@ModelAttribute("userInfo") UserInfoSession userInfoSession) {
        return "user/updateProfile";
    }

    @PutMapping("/user/profile")
    public String updateUserProfile(
            @RequestParam("nickname") String nickname,
            @RequestParam("profileImg") MultipartFile profileImage,
            @ModelAttribute("userInfo") UserInfoSession userInfoSession) throws IOException {

        if(profileImage.isEmpty()){
            userService.updateUserProfile(User.builder()
                    .nickname(nickname)
                    .email(userInfoSession.getEmail())
                    .build());
        } else{
            String uploadDirectory = "src/main/resources/static/images/profile";
            String previousProfileImagePath = userInfoSession.getProfileImage();
            if(!previousProfileImagePath.isEmpty() && !previousProfileImagePath.equals("default_profile.png")){
                imageService.deleteImage(uploadDirectory, previousProfileImagePath);
            }
            String profileImageString = imageService.saveImageToStorage(uploadDirectory, profileImage);

            userService.updateUserProfile(User.builder()
                    .profileImage(profileImageString)
                    .nickname(nickname)
                    .email(userInfoSession.getEmail())
                    .build());

            userInfoSession.setProfileImage(profileImageString);
        }

        userInfoSession.setNickname(nickname);
        return "redirect:/user/info";
    }


}
