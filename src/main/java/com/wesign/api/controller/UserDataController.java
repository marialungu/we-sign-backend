package com.wesign.api.controller;

import com.asprise.ocr.Ocr;
import com.wesign.api.dto.UserDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
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


    @PostMapping("/user-data/")
    @ResponseBody
    public UserDataDto retrieveUserData(MultipartFile file) throws IOException {
        File image = File.createTempFile(file.getOriginalFilename(),".jpg");
        file.transferTo(image);

        String s = ocr.recognize(new File[] {image},
                Ocr.RECOGNIZE_TYPE_TEXT, Ocr.OUTPUT_FORMAT_PLAINTEXT);

        log.info("Recognized \n{}",s);

        UserDataDto userDataDto = new UserDataDto();

        String[] parts = s.split("IDRO");
        if (parts.length > 1){
            String[] subParts = parts[1].split("<<<<<<<");
            String[] nameParts = subParts[0].split("<<");
            String firstName = nameParts[0].substring(1, nameParts[0].length());
            String[] lastNameParts = nameParts[1].split("<");
            StringJoiner sj = new StringJoiner(" ");
            String lastName = Arrays.stream(lastNameParts).collect(Collectors.joining(" "));

            String[] seriesSub = subParts[1].split("<");
            String series = seriesSub[0];

            Pattern p = Pattern.compile("([0-9].)");
            Matcher m = p.matcher(s);
            String cnp = "";
            if (m.find()) {
                cnp = m.group(0);
            }


            userDataDto.setFirstname(firstName);
            userDataDto.setLastName(lastName);
            userDataDto.setSeries(series.substring(1, 3));
            userDataDto.setNumber(series.substring(3, 8));
            userDataDto.setCnp(cnp);
        }

        return userDataDto;

    }
}
