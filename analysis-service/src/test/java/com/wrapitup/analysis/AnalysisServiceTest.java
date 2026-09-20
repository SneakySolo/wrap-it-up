package com.wrapitup.analysis;

import com.wrapitup.analysis.analyzer.*;
import com.wrapitup.analysis.engine.PersonalityEngine;
import com.wrapitup.common.domain.*;
import com.wrapitup.common.event.payload.WrapAnalysisCompletedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for all analysis components.
 */
public class AnalysisServiceTest {

    private ArtistAnalyzer artistAnalyzer;
    private AlbumAnalyzer albumAnalyzer;
    private GenreAnalyzer genreAnalyzer;
    private DiscoveryAnalyzer discoveryAnalyzer;
    private PersonalityEngine personalityEngine;

    @BeforeEach
    void setUp() {
        artistAnalyzer = new ArtistAnalyzer();
        albumAnalyzer = new AlbumAnalyzer();
        genreAnalyzer = new GenreAnalyzer();
        discoveryAnalyzer = new DiscoveryAnalyzer();
        personalityEngine = new PersonalityEngine();
    }

    // ===== ARTIST ANALYZER TESTS =====

    @Test
    void testGetTopArtist() {
        List<SpotifyArtist> artists = Arrays.asList(
                SpotifyArtist.builder().id("1").name("Top Artist").genres(Arrays.asList("hip hop")).build(),
                SpotifyArtist.builder().id("2").name("Second Artist").genres(Arrays.asList("rap")).build()
        );

        SpotifyArtist topArtist = artistAnalyzer.getTopArtist(artists);

        assertNotNull(topArtist);
        assertEquals("1", topArtist.getId());
        assertEquals("Top Artist", topArtist.getName());
    }

    @Test
    void testGetTopArtistEmpty() {
        SpotifyArtist topArtist = artistAnalyzer.getTopArtist(new ArrayList<>());

        assertNotNull(topArtist);
        assertNull(topArtist.getId());
    }

    @Test
    void testRisingStar() {
        // Create short-term artists: Artist A at rank 1, Artist B at rank 2
        SpotifyArtist artistA = SpotifyArtist.builder().id("a1").name("Artist A").build();
        SpotifyArtist artistB = SpotifyArtist.builder().id("b1").name("Artist B").build();
        List<SpotifyArtist> shortTermArtists = Arrays.asList(artistA, artistB);

        // Create long-term artists: Artist A at rank 5, Artist B at rank 1
        SpotifyArtist artistALT = SpotifyArtist.builder().id("a1").name("Artist A").build();
        SpotifyArtist artistBLT = SpotifyArtist.builder().id("b1").name("Artist B").build();
        List<SpotifyArtist> longTermArtists = Arrays.asList(artistBLT, artistALT);

        // Artist A: short=20, long=16, growth=4
        // Artist B: short=19, long=20, growth=-1
        // Artist A should be rising star
        SpotifyArtist risingStar = artistAnalyzer.getRisingStar(shortTermArtists, longTermArtists);

        assertEquals("a1", risingStar.getId());
        assertEquals("Artist A", risingStar.getName());
    }

    @Test
    void testRisingStarProtectionRule() {
        // Create short-term artists with 20 slots
        List<SpotifyArtist> shortTermArtists = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            shortTermArtists.add(SpotifyArtist.builder()
                    .id("artist" + i)
                    .name("Artist " + i)
                    .build());
        }

        // Create long-term artists
        List<SpotifyArtist> longTermArtists = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            longTermArtists.add(SpotifyArtist.builder()
                    .id("long" + i)
                    .name("Long Artist " + i)
                    .build());
        }

        SpotifyArtist risingStar = artistAnalyzer.getRisingStar(shortTermArtists, longTermArtists);

        // Rising star should be one of the top 10 in short-term
        // Verify it's not null and is from the eligible rank
        assertNotNull(risingStar);
        assertTrue(shortTermArtists.stream()
                .limit(10)
                .anyMatch(a -> a.getId().equals(risingStar.getId())));
    }

    @Test
    void testArtistLoyalty() {
        Map<String, Integer> artistCounts = new HashMap<>();
        artistCounts.put("artist1", 18); // 18/50 = 0.36
        artistCounts.put("artist2", 10); // 10/50 = 0.20
        artistCounts.put("artist3", 7);  // 7/50 = 0.14
        artistCounts.put("artist4", 15); // 15/50 = 0.30
        // HHI = 0.36² + 0.20² + 0.14² + 0.30² = 0.1296 + 0.04 + 0.0196 + 0.09 = 0.2792
        // Loyalty score ≈ 28

        int loyalty = artistAnalyzer.calculateArtistLoyalty(artistCounts);

        assertTrue(loyalty >= 25 && loyalty <= 35);
        assertTrue(loyalty >= 0 && loyalty <= 100);
    }

    @Test
    void testArtistLoyaltyHighConcentration() {
        Map<String, Integer> artistCounts = new HashMap<>();
        artistCounts.put("artist1", 40); // Heavily concentrated on one artist
        artistCounts.put("artist2", 10);
        // HHI = 0.8² + 0.2² = 0.64 + 0.04 = 0.68
        // Loyalty score ≈ 68

        int loyalty = artistAnalyzer.calculateArtistLoyalty(artistCounts);

        assertTrue(loyalty >= 65);
        assertTrue(loyalty <= 100);
    }

    // ===== ALBUM ANALYZER TESTS =====

    @Test
    void testAlbumAnalysis() {
        List<SpotifyTrack> tracks = new ArrayList<>();
        // Album 1: 3 tracks at ranks 0, 1, 2 (weights: 50, 49, 48 = 147)
        for (int i = 0; i < 3; i++) {
            tracks.add(SpotifyTrack.builder()
                    .id("track" + i)
                    .name("Track " + i)
                    .albumId("album1")
                    .albumName("Album 1")
                    .durationMs(180000)
                    .artistId("artist1")
                    .artistName("Artist 1")
                    .albumReleaseDate(LocalDate.of(2023, 1, 1))
                    .build());
        }
        // Album 2: 2 tracks at ranks 3, 4 (weights: 47, 46 = 93)
        for (int i = 3; i < 5; i++) {
            tracks.add(SpotifyTrack.builder()
                    .id("track" + i)
                    .name("Track " + i)
                    .albumId("album2")
                    .albumName("Album 2")
                    .durationMs(180000)
                    .artistId("artist2")
                    .artistName("Artist 2")
                    .albumReleaseDate(LocalDate.of(2023, 6, 1))
                    .build());
        }

        Map<String, Object> result = albumAnalyzer.analyzeAlbums(tracks);

        assertNotNull(result);
        assertNotNull(result.get("mostListenedAlbum"));
        assertNotNull(result.get("albumConcentration"));

        double concentration = (Double) result.get("albumConcentration");
        assertEquals(3.0 / 5.0, concentration, 0.01); // 3 out of 5 tracks from album 1
    }

    @Test
    void testAlbumConcentration() {
        List<SpotifyTrack> tracks = new ArrayList<>();
        // Spread across 50 different albums (1 track each)
        for (int i = 0; i < 50; i++) {
            tracks.add(SpotifyTrack.builder()
                    .id("track" + i)
                    .albumId("album" + i)
                    .albumName("Album " + i)
                    .durationMs(180000)
                    .artistId("artist1")
                    .artistName("Artist 1")
                    .albumReleaseDate(LocalDate.of(2023, 1, 1))
                    .build());
        }

        Map<String, Object> result = albumAnalyzer.analyzeAlbums(tracks);
        double concentration = (Double) result.get("albumConcentration");

        assertEquals(1.0 / 50.0, concentration, 0.01); // Very low concentration
    }

    // ===== GENRE ANALYZER TESTS =====

    @Test
    void testTopGenre() {
        List<SpotifyArtist> artists = Arrays.asList(
                SpotifyArtist.builder().id("1").name("Artist 1").genres(Arrays.asList("hip hop", "rap")).build(),
                SpotifyArtist.builder().id("2").name("Artist 2").genres(Arrays.asList("hip hop")).build(),
                SpotifyArtist.builder().id("3").name("Artist 3").genres(Arrays.asList("pop")).build()
        );

        String topGenre = genreAnalyzer.getTopGenre(artists);

        assertEquals("Hip-Hop", topGenre);
    }

    @Test
    void testGenreNormalization() {
        List<SpotifyArtist> artists = Arrays.asList(
                SpotifyArtist.builder().id("1").name("A1").genres(Arrays.asList("dance pop")).build(),
                SpotifyArtist.builder().id("2").name("A2").genres(Arrays.asList("electropop")).build(),
                SpotifyArtist.builder().id("3").name("A3").genres(Arrays.asList("hip hop")).build()
        );

        String topGenre = genreAnalyzer.getTopGenre(artists);

        // Dance pop and electropop should normalize to Pop (20 + 19 = 39)
        // Hip hop is 18, so Pop wins
        assertEquals("Pop", topGenre);
    }

    @Test
    void testGenreDiversity() {
        List<SpotifyArtist> artists = new ArrayList<>();
        String[] genres = {"pop", "hip hop", "rock", "electronic", "jazz", "classical", "reggae", "latin"};

        // Create artists with diverse genres
        for (int i = 0; i < 20; i++) {
            artists.add(SpotifyArtist.builder()
                    .id("artist" + i)
                    .name("Artist " + i)
                    .genres(Arrays.asList(genres[i % genres.length]))
                    .build());
        }

        int diversity = genreAnalyzer.calculateGenreDiversity(artists);

        assertTrue(diversity >= 50); // Should have high diversity
        assertTrue(diversity <= 100);
    }

    @Test
    void testLowGenreDiversity() {
        List<SpotifyArtist> artists = new ArrayList<>();

        // Create artists all in the same genre
        for (int i = 0; i < 20; i++) {
            artists.add(SpotifyArtist.builder()
                    .id("artist" + i)
                    .name("Artist " + i)
                    .genres(Arrays.asList("hip hop"))
                    .build());
        }

        int diversity = genreAnalyzer.calculateGenreDiversity(artists);

        assertTrue(diversity < 50); // Should have low diversity
        assertTrue(diversity >= 0);
    }

    // ===== DISCOVERY ANALYZER TESTS =====

    @Test
    void testFamiliarityAnalysis() {
        // Recently played: Artist A, B, C (all in long-term)
        List<RecentlyPlayedItem> recentlyPlayed = Arrays.asList(
                RecentlyPlayedItem.builder()
                        .track(SpotifyTrack.builder().artistId("a1").build())
                        .playedAt("2026-09-20T10:00:00Z")
                        .build(),
                RecentlyPlayedItem.builder()
                        .track(SpotifyTrack.builder().artistId("b1").build())
                        .playedAt("2026-09-20T11:00:00Z")
                        .build(),
                RecentlyPlayedItem.builder()
                        .track(SpotifyTrack.builder().artistId("c1").build())
                        .playedAt("2026-09-20T12:00:00Z")
                        .build()
        );

        // Long-term: A, B (2 out of 3 recent are familiar)
        List<SpotifyArtist> longTermArtists = Arrays.asList(
                SpotifyArtist.builder().id("a1").name("Artist A").build(),
                SpotifyArtist.builder().id("b1").name("Artist B").build()
        );

        Map<String, Object> result = discoveryAnalyzer.analyzeFamiliarityAndDiscovery(recentlyPlayed, longTermArtists);

        double familiarityRatio = (Double) result.get("familiarityRatio");
        double discoveryRatio = (Double) result.get("discoveryRatio");

        assertEquals(2.0 / 3.0, familiarityRatio, 0.01);
        assertEquals(1.0 / 3.0, discoveryRatio, 0.01);
    }

    @Test
    void testTasteMovement() {
        // Short-term: A, B, C, D, E (5 new, none in long-term)
        List<SpotifyArtist> shortTermArtists = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            shortTermArtists.add(SpotifyArtist.builder()
                    .id("short" + i)
                    .name("Short Artist " + i)
                    .build());
        }

        // Long-term: F, G, H, I, J (5 different, none in short-term)
        List<SpotifyArtist> longTermArtists = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            longTermArtists.add(SpotifyArtist.builder()
                    .id("long" + i)
                    .name("Long Artist " + i)
                    .build());
        }

        double tasteMovement = discoveryAnalyzer.calculateTasteMovement(shortTermArtists, longTermArtists);

        assertEquals(1.0, tasteMovement, 0.01); // Completely different taste
    }

    @Test
    void testTasteMovementStable() {
        // Short-term and long-term: same 20 artists
        List<SpotifyArtist> artists = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            artists.add(SpotifyArtist.builder()
                    .id("artist" + i)
                    .name("Artist " + i)
                    .build());
        }

        double tasteMovement = discoveryAnalyzer.calculateTasteMovement(artists, artists);

        assertEquals(0.0, tasteMovement, 0.01); // Completely stable taste
    }

    // ===== PERSONALITY ENGINE TESTS =====

    @Test
    void testPersonalityObsessive() {
        WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                82, // artistLoyalty >= 80
                50,
                0.55, // albumConcentration >= 0.50
                0.2,
                0.7,
                0.3
        );

        assertEquals("OBSESSIVE", personality.getCategory());
    }

    @Test
    void testPersonalityAlbumPerson() {
        WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                65, // artistLoyalty < 80
                50,
                0.45, // albumConcentration >= 0.40
                0.2,
                0.7,
                0.3
        );

        assertEquals("ALBUM_PERSON", personality.getCategory());
    }

    @Test
    void testPersonalityLoyalist() {
        WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                72, // artistLoyalty >= 70
                50,
                0.35, // albumConcentration < 0.40
                0.2,
                0.7,
                0.3
        );

        assertEquals("LOYALIST", personality.getCategory());
    }

    @Test
    void testPersonalityDiscoverer() {
        WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                40,
                50,
                0.25,
                0.2,
                0.3,
                0.55 // discoveryRatio >= 0.50
        );

        assertEquals("DISCOVERER", personality.getCategory());
    }

    @Test
    void testPersonalityChaosListener() {
        WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                40,
                70, // genreDiversity >= 65
                0.25,
                0.2,
                0.4,
                0.55 // discoveryRatio >= 0.50 (DISCOVERER takes priority before CHAOS_LISTENER)
        );

        // With discoveryRatio >= 0.50, DISCOVERER is matched before CHAOS_LISTENER
        // To test CHAOS_LISTENER, we need discoveryRatio < 0.50 but >= 0.45
        assertEquals("DISCOVERER", personality.getCategory());
    }

    @Test
    void testPersonalityChaoListenerActual() {
        // To actually match CHAOS_LISTENER: genreDiversity >= 65 AND discoveryRatio >= 0.45 AND discoveryRatio < 0.50
        WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                40,
                72, // genreDiversity >= 65
                0.25,
                0.2,
                0.55, // familiarityRatio
                0.45  // discoveryRatio >= 0.45 but < 0.50
        );

        assertEquals("CHAOS_LISTENER", personality.getCategory());
    }

    @Test
    void testPersonalityExplorer() {
        WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                50, // artistLoyalty < 60
                72, // genreDiversity >= 70
                0.25,
                0.3,
                0.6,
                0.4
        );

        assertEquals("EXPLORER", personality.getCategory());
    }

    @Test
    void testPersonalityDefault() {
        // To reach DEFAULT, all higher-priority conditions must be false:
        // - artistLoyalty < 80 (not OBSESSIVE)
        // - albumConcentration < 0.40 (not ALBUM_PERSON)
        // - artistLoyalty < 70 (not LOYALIST)
        // - discoveryRatio < 0.50 (not DISCOVERER)
        // - NOT (genreDiversity >= 65 AND discoveryRatio >= 0.45) (not CHAOS_LISTENER)
        // - NOT (genreDiversity >= 70 AND artistLoyalty < 60) (not EXPLORER)
        WrapAnalysisCompletedPayload.PersonalityResult personality = personalityEngine.classify(
                55,      // artistLoyalty < 70
                55,      // genreDiversity < 70
                0.25,    // albumConcentration < 0.40
                0.2,
                0.6,     // familiarityRatio
                0.35     // discoveryRatio < 0.50
        );

        assertEquals("LISTENER", personality.getCategory());
    }
}