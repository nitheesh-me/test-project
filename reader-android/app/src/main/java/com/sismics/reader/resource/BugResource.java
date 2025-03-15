package com.sismics.reader.resource;

import android.content.Context;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/**
 * Access to /bugs API, similar to SubscriptionResource.
 */
@Path("/bugs")
public class BugResource extends BaseResource {

    /**
     * POST /bugs to create a new bug.
     */
    public static void create(Context context, String description, JsonHttpResponseHandler responseHandler) {
        init(context);
        RequestParams params = new RequestParams();
        params.put("description", description);
        // Adjust path according to your endpoint
        client.post(getApiUrl(context) + "/bugs", params, responseHandler);
    }

    /**
     * DELETE /bugs/{id} to delete a bug.
     */
    public static void delete(Context context, String bugId, JsonHttpResponseHandler responseHandler) {
        init(context);
        // Adjust path according to your endpoint
        client.delete(getApiUrl(context) + "/bugs/" + bugId, null, responseHandler);
    }

    /**
     * POST /bugs/{id}/resolve to mark a bug as resolved.
     */
    public static void resolve(Context context, String bugId, JsonHttpResponseHandler responseHandler) {
        init(context);
        // Adjust path according to your endpoint
        client.post(getApiUrl(context) + "/bugs/" + bugId + "/resolve", new RequestParams(), responseHandler);
    }

    /**
     * GET /bugs to fetch a list of bugs.
     */
    public static void list(Context context, JsonHttpResponseHandler responseHandler) {
        init(context);
        // Adjust path according to your endpoint
        client.get(getApiUrl(context) + "/bugs", null, responseHandler);
    }
}