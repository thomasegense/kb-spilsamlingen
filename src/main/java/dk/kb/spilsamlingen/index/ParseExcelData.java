package dk.kb.spilsamlingen.index;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class ParseExcelData {

    public static void main(String[] args) throws Exception {

        String file = "/home/teg/eclipse-workspace/kb-spilsamlingen/doc/SpilregistrantV.03.csv";
        List<CSVRecord> csvData = getCsvData(file);

        parse(csvData);

    }

    private static List<CSVRecord> getCsvData(String fileName) throws IOException {
        File csvFile = new File(fileName);
        
        FileReader reader = null;
        try {
            reader = new FileReader(csvFile, Charset.forName("ISO-8859-1"));
            CSVFormat format = CSVFormat.Builder.create().setDelimiter(new Character(';')).build();
            Iterable<CSVRecord> recordsIt = format.parse(new FileReader(fileName));
            
            List<CSVRecord> recordList = convert(recordsIt);
            System.out.println("size:"+recordList.size());
            parse(recordList);
            return recordList;
            // Add the call to close method
        } catch (IOException e) {
            // Log the exception
            return Collections.emptyList();
        } finally {
            if (reader != null)
                reader.close();
        }

       
    }

    
    public static ArrayList<CSVRecord> convert(Iterable<CSVRecord>  it){
        
        ArrayList<CSVRecord>  list = new  ArrayList<CSVRecord> ();
        Iterator<CSVRecord> iterator = it.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        
        return list;
    }
    
    
    public static void parse(List<CSVRecord> csvRecords) {
        String headerValue;
        String value;

        String titleTemp=null;
        CSVRecord header = csvRecords.get(0);
        for (int i = 1; i < csvRecords.size(); i++) { // Skipping header index 0
            int column = 0;
            try {
                
                CSVRecord r = csvRecords.get(i);
                // INST
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // PREF
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // År
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Title
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                titleTemp=value;
                column++;

                // Platform
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Platform/OS version
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // udvikler
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // udgiver
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Spillets url
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Spillets url <- MULTIVALUED
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // omtale URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Anmeldelse URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Video
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Metascore
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Genre
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Noter
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;
                // Cover
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // unikke titler
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Fysisk medie
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Online
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Mobil(portal)
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Konsolportal + steam
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Sorteringsår
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // I Netarkivet
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Netarkivering - noter
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Itunes Info
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Alternative titler
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Medieformat
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Datamedie
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Gamejam spil
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Arrangement
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Arrangement spil URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Taste-selv spil
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Printet i
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Kildekode
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Kildekode URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Licens
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Licens URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Serie
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Tags
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Barcode
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // ISBN
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // EAN
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Opstilling
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // DFI ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // WikiData ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Apple ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Google Play ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Steam APPID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // GOG ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Mobygames ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Playright ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // IGDB ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // UVList ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // GiantBomb ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // OGDB ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // IndieDB
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Itch.io URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // GameJolt ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Ludum Dare URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // LudumData ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Newgrounds ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // PlayStation ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Microsoft ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Nintendo ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Pocket Gamer ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Metacritic ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // IMDB ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Hall of Light
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // LemonAmiga
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Lemon64
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Gamebase 64
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Plus/4 World
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // World of Spectrum
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Complete BBC Micro Games Archive
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // CASA headerValue=header.get(row);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // TUID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // IFID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // IFWiki
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // VNDB
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Software Requirements
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Hardware Requirements
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Input Devices
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Tilføjet af
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Redigeret af
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Sidst opdateret
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Udstillingens historie
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Accessionsmåde
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Accessionsdato
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Awards
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Inklusionsgrund
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // ALMA ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

//                System.out.println("------------------------");
            } catch (Exception e) {
                System.out.println("Error parsing row:"+ i +" with title:"+titleTemp+" colum" + column + " :" + e.getMessage());

            }

        }

    }

}
