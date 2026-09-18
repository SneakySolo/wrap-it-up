package com.wrapitup.spotify.service;

import com.wrapitup.common.domain.RecentlyPlayedItem;
import com.wrapitup.common.domain.SpotifyAlbum;
import com.wrapitup.common.domain.SpotifyArtist;
import com.wrapitup.common.domain.SpotifyTrack;
import com.wrapitup.spotify.dto.SpotifyApiArtistResponse;
import com.wrapitup.spotify.dto.SpotifyApiPlayHistoryResponse;
import com.wrapitup.spotify.dto.SpotifyApiTrackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Normalizes Spotify API responses into our internal domain models.
 * This isolates Spotify API specifics to this service.
 * Other services depend only on normalized domain models.
 */
@Slf4j
@Service
public class SpotifyDataNormalizer {

    /**
     * Normalize Spotify artist response to domain model.
     * Extracts only required fields:
     * - id
     * - name
     * - genres
     * - imageUrl (first/largest image if available)
     */
    public SpotifyArtist normalizeArtist(SpotifyApiArtistResponse apiArtist) {
        if (apiArtist == null) {
            return null;
        }

        String imageUrl = null;
        if (apiArtist.getImages() != null && !apiArtist.getImages().isEmpty()) {
            // Take the first image (typically the largest)
            imageUrl = apiArtist.getImages().get(0).getUrl();
        }

        return SpotifyArtist.builder()
                .id(apiArtist.getId())
                .name(apiArtist.getName())
                .genres(apiArtist.getGenres() != null ? apiArtist.getGenres() : List.of())
                .imageUrl(imageUrl)
                .build();
    }

    /**
     * Normalize Spotify track response to domain model.
     * Extracts:
     * - id
     * - name
     * - durationMs
     * - artistId and artistName (from first artist)
     * - albumId and albumName (from album)
     * - albumReleaseDate
     * - imageUrl (from album images if available)
     */
    public SpotifyTrack normalizeTrack(SpotifyApiTrackResponse apiTrack) {
        if (apiTrack == null) {
            return null;
        }

        // Extract primary artist (first in the list)
        String artistId = null;
        String artistName = null;
        if (apiTrack.getArtists() != null && !apiTrack.getArtists().isEmpty()) {
            SpotifyApiArtistResponse primaryArtist = apiTrack.getArtists().get(0);
            artistId = primaryArtist.getId();
            artistName = primaryArtist.getName();
        }

        // Extract album info
        String albumId = null;
        String albumName = null;
        String albumReleaseDate = null;
        String imageUrl = null;

        if (apiTrack.getAlbum() != null) {
            albumId = apiTrack.getAlbum().getId();
            albumName = apiTrack.getAlbum().getName();
            albumReleaseDate = apiTrack.getAlbum().getReleaseDate();

            // Get album image
            if (apiTrack.getAlbum().getImages() != null && !apiTrack.getAlbum().getImages().isEmpty()) {
                imageUrl = apiTrack.getAlbum().getImages().get(0).getUrl();
            }
        }

        LocalDate releaseDate = null;
        if (albumReleaseDate != null) {
            try {
                releaseDate = LocalDate.parse(albumReleaseDate);
            } catch (Exception e) {
                // If parsing fails, leave as null
                log.debug("Failed to parse album release date: {}", albumReleaseDate);
            }
        }

        return SpotifyTrack.builder()
                .id(apiTrack.getId())
                .name(apiTrack.getName())
                .durationMs(apiTrack.getDurationMs() != null ? apiTrack.getDurationMs().intValue() : 0)
                .artistId(artistId)
                .artistName(artistName)
                .albumId(albumId)
                .albumName(albumName)
                .albumReleaseDate(releaseDate)
                .imageUrl(imageUrl)
                .build();
    }

    /**
     * Normalize a list of Spotify artist responses.
     */
    public List<SpotifyArtist> normalizeArtists(List<SpotifyApiArtistResponse> apiArtists) {
        if (apiArtists == null) {
            return List.of();
        }

        return apiArtists.stream()
                .map(this::normalizeArtist)
                .collect(Collectors.toList());
    }

    /**
     * Normalize a list of Spotify track responses.
     */
    public List<SpotifyTrack> normalizeTracks(List<SpotifyApiTrackResponse> apiTracks) {
        if (apiTracks == null) {
            return List.of();
        }

        return apiTracks.stream()
                .map(this::normalizeTrack)
                .collect(Collectors.toList());
    }

    /**
     * Normalize a recently played item.
     * Extracts track and played_at timestamp.
     */
    public RecentlyPlayedItem normalizePlayHistoryItem(SpotifyApiPlayHistoryResponse apiItem) {
        if (apiItem == null) {
            return null;
        }

        SpotifyTrack track = normalizeTrack(apiItem.getTrack());

        return RecentlyPlayedItem.builder()
                .track(track)
                .playedAt(apiItem.getPlayedAt())
                .build();
    }

    /**
     * Normalize a list of recently played items.
     */
    public List<RecentlyPlayedItem> normalizePlayHistory(List<SpotifyApiPlayHistoryResponse> apiItems) {
        if (apiItems == null) {
            return List.of();
        }

        return apiItems.stream()
                .map(this::normalizePlayHistoryItem)
                .collect(Collectors.toList());
    }
}