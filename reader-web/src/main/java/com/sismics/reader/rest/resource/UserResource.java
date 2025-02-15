package com.sismics.reader.rest.resource;

import com.sismics.reader.core.dao.jpa.*;
import com.sismics.reader.core.model.jpa.User;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.reader.rest.service.Authentication.AuthencticationService;
import com.sismics.reader.rest.service.User.UserAuth;
import com.sismics.reader.rest.service.User.UserInfoService;
import com.sismics.reader.rest.service.User.UserRegistrationService;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.util.ValidationUtil;
import com.sismics.security.IPrincipal;
import com.sismics.util.EnvironmentUtil;
import com.sismics.util.filter.TokenBasedSecurityFilter;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;


/**
 * User REST resources.
 * 
 * @author jtremeaux
 */
@Path("/user")
public class UserResource{
    /**
     * Creates a new user.
     * 
     * @param username User's username
     * @param password Password
     * @param email E-Mail
     * @param localeId Locale ID
     * @return Response
     */
    private final AuthencticationService authencticationService;
    private final UserAuth userAuth;
    private final UserRegistrationService userRegistrationService ;
    private final UserInfoService userInfoService;

    public UserResource(@Context HttpServletRequest request) {
        this.authencticationService = new AuthencticationService(request);
        this.userInfoService = new UserInfoService(authencticationService.getRequest());
        this.userRegistrationService = new UserRegistrationService(request);
        this.userAuth = new UserAuth();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(
        @FormParam("username") String username,
        @FormParam("password") String password,
        @FormParam("locale") String localeId,
        @FormParam("email") String email) throws JSONException {

        isAdmin();

        JSONObject response = userRegistrationService.registerUser(username, password, localeId, email, authencticationService.getRequest());
        return Response.ok().entity(response).build();
    }

    private void isAdmin() throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }
        authencticationService.checkBaseFunction(BaseFunction.ADMIN);
    }

    /**
     * Updates user informations.
     * 
     * @param password Password
     * @param email E-Mail
     * @param themeId Theme
     * @param localeId Locale ID
     * @param displayTitleWeb Display only article titles (web application).
     * @param displayTitleMobile Display only article titles (mobile application).
     * @param displayUnreadWeb Display only unread titles (web application).
     * @param displayUnreadMobile Display only unread titles (mobile application).
     * @param firstConnection True if the user hasn't acknowledged the first connection wizard yet.
     * @return Response
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
        @FormParam("password") String password,
        @FormParam("email") String email,
        @FormParam("theme") String themeId,
        @FormParam("locale") String localeId,
        @FormParam("display_title_web") Boolean displayTitleWeb,
        @FormParam("display_title_mobile") Boolean displayTitleMobile,
        @FormParam("display_unread_web") Boolean displayUnreadWeb,
        @FormParam("display_unread_mobile") Boolean displayUnreadMobile,
        @FormParam("narrow_article") Boolean narrowArticle,
        @FormParam("first_connection") Boolean firstConnection) throws JSONException {
        IPrincipal principal = authencticationService.getPrincipal();
        
        // Validate the input data
        password = ValidationUtil.validateLength(password, "password", 8, 50, true);
        email = ValidationUtil.validateLength(email, "email", null, 100, true);
        localeId = com.sismics.reader.rest.util.ValidationUtil.validateLocale(localeId, "locale", true);
        themeId = com.sismics.reader.rest.util.ValidationUtil.validateTheme(EnvironmentUtil.isUnitTest() ? null : authencticationService.getRequest().getServletContext(), themeId, "theme", true);

        JSONObject response = userRegistrationService.updateUser(password, email, themeId, localeId, displayTitleWeb, displayTitleMobile, displayUnreadWeb, displayUnreadMobile, narrowArticle, firstConnection, principal);
        return Response.ok().entity(response).build();
    }

    /**
     * Updates user informations.
     * 
     * @param username Username
     * @param password Password
     * @param email E-Mail
     * @param themeId Theme
     * @param localeId Locale ID
     * @param displayTitleWeb Display only article titles (web application).
     * @param displayTitleMobile Display only article titles (mobile application).
     * @param displayUnreadWeb Display only unread titles (web application).
     * @param displayUnreadMobile Display only unread titles (mobile application).
     * @return Response
     */
    @POST
    @Path("{username: [a-zA-Z0-9_]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
        @PathParam("username") String username,
        @FormParam("password") String password,
        @FormParam("email") String email,
        @FormParam("theme") String themeId,
        @FormParam("locale") String localeId,
        @FormParam("display_title_web") Boolean displayTitleWeb,
        @FormParam("display_title_mobile") Boolean displayTitleMobile,
        @FormParam("display_unread_web") Boolean displayUnreadWeb,
        @FormParam("display_unread_mobile") Boolean displayUnreadMobile,
        @FormParam("narrow_article") Boolean narrowArticle) throws JSONException {
        isAdmin();
        
        // Validate the input data
        password = ValidationUtil.validateLength(password, "password", 8, 50, true);
        email = ValidationUtil.validateLength(email, "email", null, 100, true);
        localeId = com.sismics.reader.rest.util.ValidationUtil.validateLocale(localeId, "locale", true);
        themeId = com.sismics.reader.rest.util.ValidationUtil.validateTheme(authencticationService.getRequest().getServletContext(), themeId, "theme", true);

        JSONObject response = userRegistrationService.updateUserInfo(username, password, email, themeId, localeId, displayTitleWeb, displayTitleMobile, displayUnreadWeb, displayUnreadMobile, narrowArticle);
        return Response.ok().entity(response).build();
    }


    /**
     * Checks if a username is available. Search only on active accounts.
     * 
     * @param username Username to check
     * @return Response
     */
    @GET
    @Path("check_username")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkUsername(
        @QueryParam("username") String username) throws JSONException {
        
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        
        JSONObject response = new JSONObject();
        if (user != null) {
            response.put("status", "ko");
            response.put("message", "Username already registered");
        } else {
            response.put("status", "ok");
        }
        
        return Response.ok().entity(response).build();
    }

    /**
     * This resource is used to authenticate the user and create a user session.
     * The "session" is only used to identify the user, no other data is stored in the session.
     * 
     * @param username Username
     * @param password Password
     * @param longLasted Remember the user next time, create a long lasted session.
     * @return Response
     */
    @POST
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(
        @FormParam("username") String username,
        @FormParam("password") String password,
        @FormParam("remember") boolean longLasted) throws JSONException {

        String token = userAuth.getSession(username, password, longLasted);

        JSONObject response = new JSONObject();
        int maxAge = longLasted ? TokenBasedSecurityFilter.TOKEN_LONG_LIFETIME : -1;
        NewCookie cookie = new NewCookie(TokenBasedSecurityFilter.COOKIE_NAME, token, "/", null, null, maxAge, false);
        return Response.ok().entity(response).cookie(cookie).build();
    }

    /**
     * Logs out the user and deletes the active session.
     * 
     * @return Response
     */
    @POST
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout() throws JSONException {

        userAuth.destroySession(authencticationService.getRequest());

        // Deletes the client token in the HTTP response
        JSONObject response = new JSONObject();
        NewCookie cookie = new NewCookie(TokenBasedSecurityFilter.COOKIE_NAME, null);
        return Response.ok().entity(response).cookie(cookie).build();
    }

    /**
     * Delete a user.
     * 
     * @return Response
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete() throws JSONException {
        isAdmin();
        IPrincipal principal = authencticationService.getPrincipal();
        // Delete the user
        UserDao userDao = new UserDao();
        userDao.delete(principal.getName());
        
        // Always return ok
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }
    
    /**
     * Deletes a user.
     * 
     * @param username Username
     * @return Response
     */
    @DELETE
    @Path("{username: [a-zA-Z0-9_]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("username") String username) throws JSONException {
       isAdmin();

        JSONObject response = userRegistrationService.deleteUser(username);
        return Response.ok().entity(response).build();
    }

    /**
     * Returns the information about the connected user.
     * 
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response info() throws JSONException {
        IPrincipal principal = authencticationService.getPrincipal();
        JSONObject response = userInfoService.getInfo(principal,authencticationService.getRequest());

        return Response.ok().entity(response).build();
    }

    /**
     * Returns the information about a user.
     * 
     * @param username Username
     * @return Response
     */
    @GET
    @Path("{username: [a-zA-Z0-9_]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response view(@PathParam("username") String username) throws JSONException {
        isAdmin();
        
        JSONObject response = new JSONObject();
        
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        if (user == null) {
            throw new ClientException("UserNotFound", "The user doesn't exist");
        }
        
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("theme", user.getTheme());
        response.put("locale", user.getLocaleId());
        
        return Response.ok().entity(response).build();
    }
    
    /**
     * Returns all active users.
     * 
     * @param limit Page limit
     * @param offset Page offset
     * @param sortColumn Sort index
     * @param asc If true, ascending sorting, else descending
     * @return Response
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("sort_column") Integer sortColumn,
            @QueryParam("asc") Boolean asc) throws JSONException {
       isAdmin();

        JSONObject response = userInfoService.getAllUsers(limit, offset, sortColumn, asc);

        return Response.ok().entity(response).build();
    }

}
