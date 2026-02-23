package dk.kb.spilsamlingen.index;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import dk.kb.spilsamlingen.solr.SolrFields;
import dk.kb.spilsamlingen.solr.SolrServerClient;

public class IndexExcelData {

    
    private static SolrServerClient client = new SolrServerClient("http://localhost:8983/solr/spilsamlingen/");
    
    public static void main(String[] args) throws Exception {

        String file = "/home/teg/eclipse-workspace/kb-spilsamlingen/doc/SpilregistrantV.03.csv";
        List<CSVRecord> csvData = getCsvData(file);

        index(csvData);

    }

    private static List<CSVRecord> getCsvData(String fileName) throws IOException {
        File csvFile = new File(fileName);
        
        FileReader reader = new FileReader(csvFile, Charset.forName("ISO-8859-1"));
        try {
 
            CSVFormat format = CSVFormat.Builder.create().setDelimiter(';').build();
            Iterable<CSVRecord> recordsIt = format.parse(reader);
            
            List<CSVRecord> recordList = convert(recordsIt);
            System.out.println("size:"+recordList.size());
            index(recordList);
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
    
    
    public static void index(List<CSVRecord> csvRecords) {
        String headerValue;
        String value;
        
        String titleTemp=null;
        CSVRecord header = csvRecords.get(0);
        int indexed=0;
        for (int i = 1; i < csvRecords.size(); i++) { // Skipping header index 0
            HashMap<String,Object> fields= new HashMap<String,Object>(); 
            int column = 0;
            try {
             
                String id=""+i;
                fields.put(SolrFields.ID,id);//Solr identifier
                
                CSVRecord r = csvRecords.get(i);
                // INST
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.INST,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;
               
                
                // PREF
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.PREF,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // ID
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.SPIL_ID,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // År
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.AAR,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Title
                headerValue = header.get(column);
                value = r.get(column);
                System.out.println(i + ":" + headerValue + ":" + value);

                fields.put(SolrFields.TITEL,value);
                titleTemp=value;
                column++;

                // Platform
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.PLATFORM,value);
                column++;

                // Platform/OS version
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.PLATFORM_OS_VERSION,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // udvikler
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.UDVIKLER,value);
                column++;

                // udgiver
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.UDGIVER,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Spillets url
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.SPIL_URL,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Udviklers_url <- MULTIVALUED
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.UDVIKLERS_URL,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // omtale URL
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.OMTALE_URL,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Anmeldelse URL
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.ANMELDELSE_URL,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Video
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.VIDEO,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Metascore
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.METASCORE,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Genre
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.GENRE,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Noter
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.NOTER,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Cover
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.COVER,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // unikke titler
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.UNIKKE_TITLER,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Fysisk medie
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.FYSISK_MEDIE,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Online
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.ONLINE,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Mobil(portal)
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.MOBILE_PORTAL,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Konsolportal + steam
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.KONSOLPORTAL_STEAM,value);
                column++;

                // Sorteringsår
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.SORTRINGS_AAR,value);
                column++;

                // I Netarkivet
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.I_NETARKIVET,value);
                column++;

                // Netarkivering - noter
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.NETARKIVERING_NOTER,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Itunes Info
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.ITUNES_INFO,value);
                column++;

                // Alternative titler
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.ALTERNATIVE_TITLER,value);
                column++;

                // Medieformat
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.MEDIEFORMAT,value);                
                column++;

                // Datamedie
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.DATAMEDIE,value);
                column++;

                // Gamejam spil
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.GAMEJAM_SPIL,value);
                column++;

                // Arrangement
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.ARRANGEMENT,value);
                column++;

                // Arrangement spil URL
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.ARRANGEMENT_SPIL,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Taste-selv spil
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.TAST_SELV_SPIL,value);
                column++;
                

                // Printet i
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.PRINTER_I,value);
                column++;

                // Kildekode
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.KILDEKODE,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Kildekode URL
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.KILDEKODE_URL,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Licens
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.LICENS,value);
                column++;

                // Licens URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.LICENS_URL,value);
                column++;

                // Serie
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.SERIE,value);
                column++;

                // Tags
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.TAGS,value);
                column++;

                // Barcode
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.BARCODE,value);
                column++;

                // ISBN
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.ISBN,value);
                column++;

                // EAN
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.EAN,value);
                column++;

                // Opstilling
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.OPSTILLING,value);
                column++;

                // DFI ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.DFI_ID,value);
                column++;

                // WikiData ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.WIKIDATA_ID,value);
                column++;

                // Apple ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.APPLE_ID,value);
                column++;

                // Google Play ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.GOOLE_PLAY_ID,value);
                column++;

                // Steam APPID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.STEAM_APP_ID,value);
                column++;

                // GOG ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.GOG_ID,value);
                column++;

                // Mobygames ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.MOBYGAMES_ID,value);
                column++;

                // Playright ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.PLAYRIGHT_ID,value);
                column++;

                // IGDB ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.IGDB_ID,value);
                column++;

                // UVList ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.UVLIST_ID,value);
                column++;

                // GiantBomb ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.GIANTBOMB_ID,value);
                column++;

                // OGDB ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.OGDB_ID,value);
                column++;

                // IndieDB
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.INDIE_DB,value);
                column++;

                // Itch.io URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.ITCH_IO_URL,value);
                column++;

                // GameJolt ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.GAMEJOLT_ID,value);
                column++;

                // Ludum Dare URL
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.LUDUM_DARE_URL,value);
                column++;

                // LudumData ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.LUDUM_DATA_ID,value);
                column++;

                // Newgrounds ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.NEWGROUNDS_ID,value);
                column++;

                // PlayStation ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.PLAYSTATION_ID,value);
                column++;

                // Microsoft ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.MICROSOFT_ID,value);
                column++;

                // Nintendo ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.NINTENDO_ID,value);
                column++;

                // Pocket Gamer ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.POCKET_GAMER_ID,value);
                column++;

                // Metacritic ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.METACRITIC_ID,value);
                column++;
                
                // IMDB ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.IMDB_ID,value);
                column++;

                // Hall of Light
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.HALL_OF_LIGHT,value);
                column++;

                // LemonAmiga
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.LEMON_AMIGA,value);
                column++;

                // Lemon64
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.LEMON_64,value);
                column++;

                // Gamebase 64
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.GAMEBASE_64,value);
                column++;

                // Plus/4 World
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.PLUS4_WORLD,value);
                column++;

                // World of Spectrum
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.WORLD_OF_SPECTRUM,value);
                column++;

                // Complete BBC Micro Games Archive
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.COMPLETE_BBC_MICRO_GAMES_ARCHIVE,value);
                column++;

                // CASA
                headerValue=header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
// TODO                fields.put(SolrFields.CASA,value);
                column++;

                // TUID
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.TU_ID,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // IFID
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.IF_ID,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // IFWiki
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.IF_WIKI,value);
                column++;

                // VNDB
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.VNDB,value);
                column++;

                // Software Requirements
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.SOFTWARE_REQUIREMENT,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Hardware Requirements
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.HARDWARE_REQUIREMENTS,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Input Devices
                headerValue = header.get(column);
                value = r.get(column);
                fields.put(SolrFields.INPUT_DEVICE,value);
                //System.out.println(i + ":" + headerValue + ":" + value);
                column++;

                // Tilføjet af
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.TILFOEJET_AF,value);
                column++;

                // Redigeret af
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.REDIGERET_AF,value);
                column++;

                // Sidst opdateret
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.SIDST_OPDATERET,value);
                column++;

                // Udstillingens historie
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.UDSTILLINGENS_HISTORIE,value);
                column++;

                // Accessionsmåde
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.ACCESSIONS_MAADE,value);
                column++;

                // Accessionsdato
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.ACCESSIONS_DATO,value);
                column++;

                // Awards
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.AWARDS,value);
                column++;

                // Inklusionsgrund
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.INKLUSIONS_GRUND,value);
                column++;

                // ALMA ID
                headerValue = header.get(column);
                value = r.get(column);
                //System.out.println(i + ":" + headerValue + ":" + value);
                fields.put(SolrFields.ALMA_ID,value);
                column++;
                
                System.out.println("adding doc with id:"+id);
                client.addDocument(fields);
                indexed++;                

                
//                System.out.println("------------------------");
            } catch (Exception e) {
                System.out.println("Error parsing row:"+ i +" with title:"+titleTemp+" colum" + column + " :" + e.getMessage());

            }
           

        }
        System.out.println("Index finished. #docs="+indexed);

    }

}
