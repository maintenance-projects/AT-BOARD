package kr.co.ultari.at_board.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ProfilePhotoControllerAdvice {

    @Value("${profile.photo.url-pattern:}")
    private String profilePhotoUrlPattern;

    @ModelAttribute("profilePhotoUrlPattern")
    public String profilePhotoUrlPattern() {
        return profilePhotoUrlPattern;
    }
}
