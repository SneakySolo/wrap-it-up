package com.wrapitup.analysis.engine;

import com.wrapitup.common.event.payload.WrapAnalysisCompletedPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Personality classification engine using rule-based deterministic categories.
 *
 * Inputs to the engine:
 * - artistLoyalty (0-100)
 * - genreDiversity (0-100)
 * - albumConcentration (0.0-1.0)
 * - tasteMovement (0.0-1.0)
 * - familiarityRatio (0.0-1.0)
 * - discoveryRatio (0.0-1.0)
 *
 * Categories (in priority order):
 * 1. THE OBSESSIVE
 * 2. THE ALBUM PERSON
 * 3. THE LOYALIST
 * 4. THE DISCOVERER
 * 5. THE CHAOS LISTENER
 * 6. THE EXPLORER
 * 7. DEFAULT (if none match)
 *
 * The engine is deterministic: a user matches exactly one category.
 */
@Slf4j
@Component
public class PersonalityEngine {

    /**
     * Classifies the user into exactly one personality category based on their listening metrics.
     *
     * @param artistLoyalty score 0-100
     * @param genreDiversity score 0-100
     * @param albumConcentration value 0.0-1.0
     * @param tasteMovement value 0.0-1.0
     * @param familiarityRatio value 0.0-1.0
     * @param discoveryRatio value 0.0-1.0
     * @return PersonalityResult with category, title, and description
     */
    public WrapAnalysisCompletedPayload.PersonalityResult classify(
            int artistLoyalty,
            int genreDiversity,
            double albumConcentration,
            double tasteMovement,
            double familiarityRatio,
            double discoveryRatio) {

        log.info("Classifying personality: loyalty={}, diversity={}, album={}, taste={}, familiar={}, discovery={}",
                artistLoyalty, genreDiversity, albumConcentration, tasteMovement, familiarityRatio, discoveryRatio);

        // Priority 1: THE OBSESSIVE
        if (artistLoyalty >= 80 && albumConcentration >= 0.50) {
            return createPersonality("OBSESSIVE", "The Obsessive",
                    "You've found your favorites, and you're sticking with them. Your top artists and albums get almost all of your attention.");
        }

        // Priority 2: THE ALBUM PERSON
        if (albumConcentration >= 0.40) {
            return createPersonality("ALBUM_PERSON", "The Album Person",
                    "You're an album listener through and through. You don't just cherry-pick tracks—you commit to whole projects.");
        }

        // Priority 3: THE LOYALIST
        if (artistLoyalty >= 70) {
            return createPersonality("LOYALIST", "The Loyalist",
                    "You have your core group of favorite artists, and that's where most of your listening happens. You're devoted to what you love.");
        }

        // Priority 4: THE DISCOVERER
        if (discoveryRatio >= 0.50) {
            return createPersonality("DISCOVERER", "The Discoverer",
                    "You're always exploring new artists and sounds. More than half of what you've been listening to lately is fresh territory.");
        }

        // Priority 5: THE CHAOS LISTENER
        if (genreDiversity >= 65 && discoveryRatio >= 0.45) {
            return createPersonality("CHAOS_LISTENER", "The Chaos Listener",
                    "Your playlist is beautifully chaotic. You jump between genres and keep discovering new things.");
        }

        // Priority 6: THE EXPLORER
        if (genreDiversity >= 70 && artistLoyalty < 60) {
            return createPersonality("EXPLORER", "The Explorer",
                    "Your listening rarely stays in one lane. You're always exploring different genres and artists.");
        }

        // Priority 7: DEFAULT
        return createPersonality("LISTENER", "The Listener",
                "Your taste is uniquely yours. You blend familiarity with discovery in a way that's all you.");
    }

    /**
     * Helper method to create a personality result.
     */
    private WrapAnalysisCompletedPayload.PersonalityResult createPersonality(
            String category, String title, String description) {
        log.info("User classified as: {}", category);
        return WrapAnalysisCompletedPayload.PersonalityResult.builder()
                .category(category)
                .title(title)
                .description(description)
                .build();
    }
}