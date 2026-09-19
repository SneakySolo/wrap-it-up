package com.wrapitup.spotify.service;

import com.wrapitup.common.domain.SpotifyArtist;
import com.wrapitup.common.domain.SpotifyTrack;
import com.wrapitup.spotify.dto.SpotifyApiArtistResponse;
import com.wrapitup.spotify.dto.SpotifyApiTrackResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpotifyDataNormalizerTest {

    private SpotifyDataNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new SpotifyDataNormalizer();
    }

    @Test
    void testNormalizeArtist_WithAllFields() {
        // Arrange
        SpotifyApiArtistResponse apiArtist = SpotifyApiArtistResponse.builder()
                .id("artist123")
                .name("Taylor Swift")
                .genres(List.of("pop", "singer-songwriter"))
                .images(List.of(
                        SpotifyApiArtistResponse.ImageDto.builder()
                                .url("https://example.com/image.jpg")
                                .height(640)
                                .width(640)
                                .build()
                ))
                .build();

        // Act
        SpotifyArtist result = normalizer.normalizeArtist(apiArtist);

        // Assert
        assertNotNull(result);
        assertEquals("artist123", result.getId());
        assertEquals("Taylor Swift", result.getName());
        assertEquals(2, result.getGenres().size());
        assertEquals("https://example.com/image.jpg", result.getImageUrl());
    }

    @Test
    void testNormalizeArtist_WithoutImage() {
        // Arrange
        SpotifyApiArtistResponse apiArtist = SpotifyApiArtistResponse.builder()
                .id("artist456")
                .name("The Beatles")
                .genres(List.of("rock"))
                .images(List.of())
                .build();

        // Act
        SpotifyArtist result = normalizer.normalizeArtist(apiArtist);

        // Assert
        assertNotNull(result);
        assertEquals("artist456", result.getId());
        assertEquals("The Beatles", result.getName());
        assertNull(result.getImageUrl());
    }

    @Test
    void testNormalizeArtist_Null() {
        // Act & Assert
        assertNull(normalizer.normalizeArtist(null));
    }

    @Test
    void testNormalizeTrack_WithFullAlbumInfo() {
        // Arrange
        SpotifyApiTrackResponse apiTrack = SpotifyApiTrackResponse.builder()
                .id("track123")
                .name("Anti-Hero")
                .durationMs(201000L)
                .artists(List.of(
                        SpotifyApiArtistResponse.builder()
                                .id("artist123")
                                .name("Taylor Swift")
                                .genres(List.of("pop"))
                                .build()
                ))
                .album(SpotifyApiTrackResponse.AlbumDto.builder()
                        .id("album123")
                        .name("Midnights")
                        .releaseDate("2022-10-21")
                        .images(List.of(
                                SpotifyApiArtistResponse.ImageDto.builder()
                                        .url("https://example.com/album.jpg")
                                        .build()
                        ))
                        .build()
                )
                .build();

        // Act
        SpotifyTrack result = normalizer.normalizeTrack(apiTrack);

        // Assert
        assertNotNull(result);
        assertEquals("track123", result.getId());
        assertEquals("Anti-Hero", result.getName());
        assertEquals(201000L, result.getDurationMs());
        assertEquals("artist123", result.getArtistId());
        assertEquals("Taylor Swift", result.getArtistName());
        assertEquals("album123", result.getAlbumId());
        assertEquals("Midnights", result.getAlbumName());
        assertEquals(java.time.LocalDate.parse("2022-10-21"), result.getAlbumReleaseDate());
        assertEquals("https://example.com/album.jpg", result.getImageUrl());
    }

    @Test
    void testNormalizeTrack_WithoutArtists() {
        // Arrange
        SpotifyApiTrackResponse apiTrack = SpotifyApiTrackResponse.builder()
                .id("track456")
                .name("Unknown Track")
                .durationMs(180000L)
                .artists(List.of())
                .album(SpotifyApiTrackResponse.AlbumDto.builder()
                        .id("album456")
                        .name("Unknown Album")
                        .releaseDate("2020-01-01")
                        .build()
                )
                .build();

        // Act
        SpotifyTrack result = normalizer.normalizeTrack(apiTrack);

        // Assert
        assertNotNull(result);
        assertNull(result.getArtistId());
        assertNull(result.getArtistName());
        assertEquals("album456", result.getAlbumId());
    }

    @Test
    void testNormalizeArtists_List() {
        // Arrange
        List<SpotifyApiArtistResponse> apiArtists = List.of(
                SpotifyApiArtistResponse.builder()
                        .id("artist1")
                        .name("Artist 1")
                        .genres(List.of("genre1"))
                        .images(List.of())
                        .build(),
                SpotifyApiArtistResponse.builder()
                        .id("artist2")
                        .name("Artist 2")
                        .genres(List.of("genre2"))
                        .images(List.of())
                        .build()
        );

        // Act
        List<SpotifyArtist> result = normalizer.normalizeArtists(apiArtists);

        // Assert
        assertEquals(2, result.size());
        assertEquals("artist1", result.get(0).getId());
        assertEquals("artist2", result.get(1).getId());
    }

    @Test
    void testNormalizeTracks_List() {
        // Arrange
        List<SpotifyApiTrackResponse> apiTracks = List.of(
                SpotifyApiTrackResponse.builder()
                        .id("track1")
                        .name("Track 1")
                        .durationMs(180000L)
                        .artists(List.of())
                        .album(SpotifyApiTrackResponse.AlbumDto.builder()
                                .id("album1")
                                .name("Album 1")
                                .releaseDate("2020-01-01")
                                .build())
                        .build(),
                SpotifyApiTrackResponse.builder()
                        .id("track2")
                        .name("Track 2")
                        .durationMs(200000L)
                        .artists(List.of())
                        .album(SpotifyApiTrackResponse.AlbumDto.builder()
                                .id("album2")
                                .name("Album 2")
                                .releaseDate("2021-01-01")
                                .build())
                        .build()
        );

        // Act
        List<SpotifyTrack> result = normalizer.normalizeTracks(apiTracks);

        // Assert
        assertEquals(2, result.size());
        assertEquals("track1", result.get(0).getId());
        assertEquals("track2", result.get(1).getId());
    }

    @Test
    void testNormalizeArtists_Empty() {
        // Act
        List<SpotifyArtist> result = normalizer.normalizeArtists(List.of());

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    void testNormalizeArtists_Null() {
        // Act
        List<SpotifyArtist> result = normalizer.normalizeArtists(null);

        // Assert
        assertEquals(0, result.size());
    }
}