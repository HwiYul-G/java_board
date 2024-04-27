package com.y.java_board.controller;

import com.y.java_board.config.UserInfoSession;
import com.y.java_board.domain.Article;
import com.y.java_board.domain.Comment;
import com.y.java_board.domain.User;
import com.y.java_board.dto.UserDto;
import com.y.java_board.service.ArticleService;
import com.y.java_board.service.CommentService;
import com.y.java_board.service.ImageService;
import com.y.java_board.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Controller
@RequiredArgsConstructor
@SessionAttributes("userInfo")
public class UserController {
    private final String PROFILE_IMG_DIRECTORY = "src/main/resources/static/images/profile";

    private final UserService userService;
    private final ImageService imageService;
    private final CommentService commentService;
    private final ArticleService articleService;
    private final HttpSession session;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping("/user/register")
    public String showSignUpForm(ModelMap model) {
        model.addAttribute("userDto", new User());
        return "user/signup";
    }

    @PostMapping("/user/register")
    public String signUp(UserDto userDto, RedirectAttributes redirectAttributes) {
        try {
            User registered = userService.registerNewUserAccount(userDto);
            return "redirect:/";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/register";
        }
    }

    @GetMapping("/user/info")
    public String showUserInfo(
            ModelMap model,
            @ModelAttribute("userInfo") UserInfoSession userInfoSession,
            @RequestParam(name = "commentPage", required = false, defaultValue = "1") int commentPage,
            @RequestParam(name = "articlePage", required = false, defaultValue = "1") int articlePage) {
        try {
            byte[] profileImageBytes = imageService.getImage(PROFILE_IMG_DIRECTORY, userInfoSession.getProfileImage()); // Unhandled exception : java.io.IOException
            String profileImageBase64 = Base64.getEncoder().encodeToString(profileImageBytes);
            model.put("profileImage", profileImageBase64);
            articlesByPage(model, articlePage, userInfoSession.getNickname());
            commentsByPage(model, commentPage, userInfoSession.getNickname());
        } catch (IOException e) {
            model.put("profileImage", "");
            System.err.println(e.getMessage());
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

        if (profileImage.isEmpty()) {
            userService.updateUserProfile(User.builder()
                    .nickname(nickname)
                    .email(userInfoSession.getEmail())
                    .build());
        } else {
            String previousProfileImagePath = userInfoSession.getProfileImage();
            if (!previousProfileImagePath.isEmpty() && !previousProfileImagePath.equals("default_profile.png")) {
                imageService.deleteImage(PROFILE_IMG_DIRECTORY, previousProfileImagePath);
            }
            String profileImageString = imageService.saveImageToStorage(PROFILE_IMG_DIRECTORY, profileImage);

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


    @DeleteMapping("/user/{email}")
    public String deleteUser(
            @PathVariable("email") String email,
            @ModelAttribute("userInfo") UserInfoSession userInfoSession
    ) throws IOException {
        String profileImg = userInfoSession.getProfileImage();
        userService.deleteUser(email);
        if (!profileImg.equals("default_profile.png"))
            imageService.deleteImage(PROFILE_IMG_DIRECTORY, profileImg);
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/commentInfo/{commentPageNumber}")
    public String commentsByPage(ModelMap model, @PathVariable("commentPageNumber") int currentPage, String nickname) {
        Page<Comment> page = commentService.findCommentByNicknameAndPaginate(nickname, currentPage);
        long totalItems = page.getTotalElements();
        int totalPages = page.getTotalPages();

        List<Comment> myComments = page.getContent();

        model.put("myCommentTotalItems", totalItems);
        model.put("myCommentTotalPages", totalPages);
        model.put("myCommentCurrentPage", currentPage);
        model.put("myComments", myComments);
        return "redirect:/user/info";
    }

    @GetMapping("/articleInfo/{articlePageNumber}")
    public String articlesByPage(ModelMap model, @PathVariable("articlePageNumber") int currentPage, String nickname) {
        Page<Article> page = articleService.findPagingArticlesByWriter(currentPage, nickname);
        long totalItems = page.getTotalElements();
        int totalPages = page.getTotalPages();

        List<Article> myArticles = page.getContent();

        model.put("myArticleTotalItems", totalItems);
        model.put("myArticleTotalPages", totalPages);
        model.put("myArticleCurrentPage", currentPage);
        model.put("myArticles", myArticles);

        return "redirect:/user/info";
    }

    @DeleteMapping("/comments/{commentId}")
    public String deleteCommentById(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return "redirect:/user/info";
    }

    @GetMapping("/user/password")
    public String showUserPasswordForm() {
        return "user/updatePassword";
    }

    @PutMapping("/user/updatePassword")
    public String updatePassword(
            @RequestParam("password") String password,
            @RequestParam("oldPassword") String oldPassword,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findUserByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!userService.checkIfValidOldPassword(user, oldPassword)) {
            redirectAttributes.addFlashAttribute("isWrong", true);
            return "redirect:/user/password";
        }
        userService.changeUserPassword(user, password);
        return "redirect:/user/info";
    }


}
