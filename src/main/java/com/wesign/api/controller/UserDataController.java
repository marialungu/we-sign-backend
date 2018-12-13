package com.wesign.api.controller;

import com.asprise.ocr.Ocr;
import com.wesign.api.dto.UserDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class UserDataController {

    private Ocr ocr;
    public String base64image;

    @PostConstruct
    private void postConstruct(){
        Ocr.setUp(); // one time setup
        ocr = new Ocr(); // create a new OCR engine
        ocr.startEngine("eng", Ocr.SPEED_SLOW); // English
    }

    @PreDestroy
    private void preDestroy(){
        ocr.stopEngine();
    }

    @PostMapping("/get-id-photo")
    @ResponseBody
    public UserDataDto retrieveUserData(MultipartFile ionicfile) throws IOException {
        File image = File.createTempFile(ionicfile.getOriginalFilename(),".jpg");
        ionicfile.transferTo(image);

        String s = ocr.recognize(new File[] {image},
                Ocr.RECOGNIZE_TYPE_TEXT, Ocr.OUTPUT_FORMAT_PLAINTEXT);

        log.info("Recognized \n{}",s);

        UserDataDto userDataDto = new UserDataDto();

        setUserData(s, userDataDto);

        return userDataDto;

    }

    private void setUserData(String s, UserDataDto dto){
        String firstName="", lastName="", series="", number="";
        Pattern p = Pattern.compile("IDRO[UM]([A-Za-z0-9]+)[< ]+(.+)[<]+.+[\\n\\r ](.+)[<]");
        Matcher m = p.matcher(s);
        if (m.find()){
            try{
                firstName = m.group(1);
            }
            catch (Exception e){
                log.info("failed to match firstnme");
            }

            try{
                lastName = m.group(2);
                lastName = lastName.replace("<"," ").trim();
            }
            catch (Exception e){
                log.info("failed to match firstnme");
            }

            try{
                String seriesParts = m.group(3);
                if (seriesParts.length() > 3){
                    series = seriesParts.substring(0,2);
                    number = seriesParts.substring(2,seriesParts.length());
                }
            }
            catch (Exception e){
                log.info("failed to match series");
            }

            log.info("{} {} {} {}", firstName, lastName, series, number);
            dto.setFirstname(firstName);
            dto.setLastName(lastName);
            dto.setNumber(number);
            dto.setSeries(series);
        }
    }
}
