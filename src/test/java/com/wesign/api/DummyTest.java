package com.wesign.api;


import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DummyTest {

    @Test
    public void testMatching(){
        String input = "- n.” ._.__.-._\n" +
                "f _ ROMANIA\n" +
                "CAHTE CARTE DE IDENTITATE IDENTITY\n" +
                "D'IDENTITE SERIA RX NR 863299 CARD\n" +
                "CNP 29602274500297 2 62Q31\n" +
                "Nume/Nom/Last name ‘ ‘\n" +
                "LUNGU\n" +
                "Prenume/Prenom/First name\n" +
                "ANA-MARIA-GAZRIELA\n" +
                "CetagenIe/Nauonante/Nationanty Sé'i/S‘a‘AQISex'\n" +
                "Roméné / ROU F\n" +
                "Lee na$tere/Lieu de naissance/Place of birth\n" +
                "Mun.Bucuresti Sec.5“\n" +
                "Domiciliu/Adresse/Address \\ ‘~ ‘\n" +
                "Mun.3ucure§ti Sec.13Ld.Idn MihaLache -\n" +
                "nr.331 bL.13 sc.A et.2 ap.10 v\n" +
                "D Emisa de/Delivree par/Issued by Valabilita!e.-Va::dxte V314 3»\n" +
                "829 @ S.P.C.E.P. Sector 1 24.02.16-27022023 I\n" +
                "IDROULUNGU<<ANA<MARIA<GA3RIELA<<<<<< ::\n" +
                "RX863299<9R0U9602278F230227024500292 ;\n" +
                "71\n" +
                "LA\n" +
                "V\n" +
                "L\n" +
                ".i\n" +
                "_i\n" +
                ".i\n" +
                ".4\n" +
                "u\n" +
                "..i";

        matchId(input);
    }

    public void matchId(String s){
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
        }
    }
}
