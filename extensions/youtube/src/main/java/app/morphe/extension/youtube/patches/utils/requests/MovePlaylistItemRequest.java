/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.utils.requests;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Objects;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.requests.Requester;

/**
 * Moves an item that is already in the queue playlist to a different position.
 * Unlike the other playlist requests this has no future cache, because the only caller
 * already runs on a background thread and needs the result before it can continue.
 */
public final class MovePlaylistItemRequest {

    private MovePlaylistItemRequest() {
    }

    /**
     * @param predecessorSetVideoId The item to place this one after, or null to move to the front.
     * @return Whether the move succeeded.
     */
    public static boolean move(String playlistId, String setVideoId,
                               @Nullable String predecessorSetVideoId,
                               Map<String, String> requestHeader) {
        Objects.requireNonNull(playlistId);
        Objects.requireNonNull(setVideoId);
        Utils.verifyOffMainThread();

        final long startTime = System.currentTimeMillis();
        Logger.printDebug(() -> "Moving queue item, setVideoId: " + setVideoId
                + ", after: " + predecessorSetVideoId);

        try {
            byte[] requestBody = PlaylistRoutes.movePlaylistItemBody(
                    playlistId, setVideoId, predecessorSetVideoId);
            HttpURLConnection connection = PlaylistRoutes.getConnection(
                    PlaylistRoutes.EDIT_PLAYLIST, requestHeader);
            connection.setFixedLengthStreamingMode(requestBody.length);
            connection.getOutputStream().write(requestBody);

            final int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Logger.printInfo(() -> "Move queue item failed with code: " + responseCode);
                return false;
            }

            JSONObject json = Requester.parseJSONObject(connection);
            return "STATUS_SUCCEEDED".equals(json.optString("status"));
        } catch (SocketTimeoutException ex) {
            Logger.printInfo(() -> "Connection timeout", ex);
        } catch (IOException ex) {
            Logger.printInfo(() -> "Network error", ex);
        } catch (Exception ex) {
            Logger.printException(() -> "move failed", ex);
        } finally {
            Logger.printDebug(() -> "Move took: " + (System.currentTimeMillis() - startTime) + "ms");
        }
        return false;
    }
}
