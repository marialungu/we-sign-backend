package com.wesign.api.controller;

import com.asprise.ocr.Ocr;
import com.wesign.api.dto.UserDataDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.text.html.parser.Entity;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
public class UserDataController {

    private static final String subscriptionKey = "011aaf8ece0c492d80319bc9a0a533a0";
    private static final String uriBaseId =
            "https://westeurope.api.cognitive.microsoft.com/face/v1.0/detect";
    private static final String uriBaseSelfie =
            "https://westeurope.api.cognitive.microsoft.com/face/v1.0/verify";
    private static final String faceAttributes = "age,gender";
    private String imageIds;
    private String imageSelfies;

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
    public UserDataDto retrieveUserData(MultipartFile ionicfileId) throws IOException {
        File image = File.createTempFile(ionicfileId.getOriginalFilename(),".jpg");
        ionicfileId.transferTo(image);

        String s = ocr.recognize(new File[] {image},
                Ocr.RECOGNIZE_TYPE_TEXT, Ocr.OUTPUT_FORMAT_PLAINTEXT);

        log.info("Recognized \n{}",s);

        UserDataDto userDataDto = new UserDataDto();

        setUserData(s, userDataDto);

        //detect face for face recognition
        try {
            String faceId = getFaceFromId(image);
            this.imageIds = faceId;
            userDataDto.setIdImage(faceId);
        } catch (URISyntaxException e) {
            log.warn("Failed to extract face id");
            e.printStackTrace();
        }

        return userDataDto;

    }

    @PostMapping("/get-selfie-photo")
    @ResponseBody
    public boolean userValidation(MultipartFile ionicfileSelfie) throws IOException {
        File image = File.createTempFile(ionicfileSelfie.getOriginalFilename(),".jpg");
        ionicfileSelfie.transferTo(image);

        //detect face for face recognition
        try {
            String faceId = getFaceFromId(image);
            this.imageSelfies = faceId;
        } catch (URISyntaxException e) {
            log.warn("Failed to extract face id");
            e.printStackTrace();
        }

        try {
            return getFaceRecognition();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean getFaceRecognition() throws URISyntaxException, IOException {
        StringEntity body = new StringEntity("{" +
                "\"faceId1\"" + ":" + "\"" + this.imageIds + "\"" + "," +
                "\"faceId2\"" + ":" +  "\"" + this.imageSelfies + "\"" + "}" , ContentType.APPLICATION_JSON);
        HttpClient httpclient = new DefaultHttpClient();

        URIBuilder builder = new URIBuilder(uriBaseSelfie);

        // Request parameters. All of them are optional.
        builder.setParameter("returnFaceId", "true");
        builder.setParameter("returnFaceLandmarks", "false");
        builder.setParameter("returnFaceAttributes", faceAttributes);


        // Prepare the URI for the REST API call.
        URI uri = builder.build();
        HttpPost request = new HttpPost(uri);

        // Request headers.
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
        request.setEntity(body);

        HttpResponse response = httpclient.execute(request);
        HttpEntity responseEntity = response.getEntity();

        if (responseEntity != null) {

            String jsonString = EntityUtils.toString(responseEntity).trim();

            JSONObject jsonObject = new JSONObject(jsonString);
            Boolean isIdentical = Boolean.parseBoolean(jsonObject.get("isIdentical").toString());
            Double confidence = Double.parseDouble(jsonObject.get("confidence").toString());

            System.out.println(isIdentical);
            System.out.println(confidence);
            return isIdentical;
        }

       return false;
    }


    private String getFaceFromId(File image) throws URISyntaxException, IOException {
        HttpClient httpclient = new DefaultHttpClient();

        URIBuilder builder = new URIBuilder(uriBaseId);

        // Request parameters. All of them are optional.
        builder.setParameter("returnFaceId", "true");
        builder.setParameter("returnFaceLandmarks", "false");
        builder.setParameter("returnFaceAttributes", faceAttributes);

        // Prepare the URI for the REST API call.
        URI uri = builder.build();
        HttpPost request = new HttpPost(uri);

        // Request headers.
        request.setHeader("Content-Type", "application/octet-stream");
        request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
        request.setEntity(new FileEntity(image));
        // Request body.

        // Execute the REST API call and get the response entity.
        HttpResponse response = httpclient.execute(request);
        HttpEntity entity = response.getEntity();

        if (entity != null) {

            String jsonString = EntityUtils.toString(entity).trim();

            JSONArray jsonArray = new JSONArray(jsonString);
            return jsonArray.getJSONObject(0).get("faceId").toString();
        }

        return null;
    }


    private void setUserData(String s, UserDataDto dto){
        String firstName="", lastName="", series="", number="";
        Pattern p = Pattern.compile("[A-Z* ]{5}([A-Za-z0-9]+)[< ]+(.+)[<]+.+[\\n\\r ](.+)[<]");
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
