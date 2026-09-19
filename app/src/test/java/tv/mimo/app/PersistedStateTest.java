package tv.mimo.app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class PersistedStateTest {

    /* ── Strings translations ── */
    @Test public void englishIsDefault(){assertEquals("Home",Strings.get("nav_home","en"));}
    @Test public void azerbaijaniTranslation(){assertEquals("Ana səhifə",Strings.get("nav_home","az"));}
    @Test public void unknownKeyReturnsKey(){assertEquals("nonexistent",Strings.get("nonexistent","en"));}
    @Test public void unknownLangFallsBackToEnglish(){assertEquals("Home",Strings.get("nav_home","fr"));}
    @Test public void allNavKeysTranslated(){
        for(String key:new String[]{"nav_home","nav_search","nav_favorites","nav_guide","nav_settings"}){
            assertNotEquals(key,Strings.get(key,"az"));}
    }
    @Test public void allPlaybackKeysTranslated(){
        for(String key:new String[]{"play_opening","play_connecting","play_live","play_buffering","play_ended","play_unavailable","play_refreshing"}){
            assertNotEquals(key,Strings.get(key,"az"));}
    }
    @Test public void allSettingsKeysTranslated(){
        for(String key:new String[]{"settings_headline","settings_sub","settings_playlists","settings_add","settings_refresh","settings_diag","settings_restore","settings_lang"}){
            assertNotEquals(key,Strings.get(key,"az"));}
    }
    @Test public void formatSubstitutionWorks(){
        String result=Strings.get("remove_playlist","en","Test Channel");
        assertEquals("Remove Test Channel?",result);
    }

    /* ── Language codes ── */
    @Test public void languageCodesAreConsistent(){
        String en=Strings.get("nav_home","en");
        String az=Strings.get("nav_home","az");
        assertNotEquals(en,az);
        assertTrue(en.length()>0);
        assertTrue(az.length()>0);
    }
}
