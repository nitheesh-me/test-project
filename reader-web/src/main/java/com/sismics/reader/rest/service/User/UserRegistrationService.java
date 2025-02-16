package com.sismics.reader.rest.service.User;

import com.sismics.reader.core.constant.Constants;
import com.sismics.reader.core.dao.jpa.*;
import com.sismics.reader.core.event.PasswordChangedEvent;
import com.sismics.reader.core.event.UserCreatedEvent;
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.model.jpa.Category;
import com.sismics.reader.core.model.jpa.User;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.reader.rest.service.Authentication.AuthenticationService;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;
import com.sismics.security.IPrincipal;
import com.sismics.util.LocaleUtil;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.Date;
import java.util.Set;

public class UserRegistrationService  {
    private final AuthenticationService authService;
    public UserRegistrationService(@Context HttpServletRequest request) {
        this.authService = new AuthenticationService(request);

    }


    public JSONObject registerUser(String username, String password, String localeId, String email, HttpServletRequest request) throws JSONException {
        // Validate the input data
        username = ValidationUtil.validateLength(username, "username", 3, 50);
        ValidationUtil.validateAlphanumeric(username, "username");
        password = ValidationUtil.validateLength(password, "password", 8, 50);
        email = ValidationUtil.validateLength(email, "email", 3, 50);
        ValidationUtil.validateEmail(email, "email");

        // Create the user
        User user = new User();
        user.setRoleId(Constants.DEFAULT_USER_ROLE);
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setDisplayTitleWeb(false);
        user.setDisplayTitleMobile(true);
        user.setDisplayUnreadWeb(true);
        user.setDisplayUnreadMobile(true);
        user.setCreateDate(new Date());

        if (localeId == null) {
            // Set the locale from the HTTP headers
            localeId = LocaleUtil.getLocaleIdFromAcceptLanguage(request.getHeader("Accept-Language"));
        }
        user.setLocaleId(localeId);

        // Create the user
        UserDao userDao = new UserDao();
        String userId;
        try {
            userId = userDao.create(user);
        } catch (Exception e) {
            if ("AlreadyExistingUsername".equals(e.getMessage())) {
                throw new ServerException("AlreadyExistingUsername", "Login already used", e);
            } else {
                throw new ServerException("UnknownError", "Unknown Server Error", e);
            }
        }

        // Create the root category for this user
        Category category = new Category();
        category.setUserId(userId);
        category.setOrder(0);

        CategoryDao categoryDao = new CategoryDao();
        categoryDao.create(category);

        // Raise a user creation event
        UserCreatedEvent userCreatedEvent = new UserCreatedEvent();
        userCreatedEvent.setUser(user);
        AppContext.getInstance().getEventBusManager().getMailEventBus().post(userCreatedEvent);

        // Always return OK
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return response;
    }


    public JSONObject updateUser(String password, String email, String themeId, String localeId, Boolean displayTitleWeb, Boolean displayTitleMobile, Boolean displayUnreadWeb, Boolean displayUnreadMobile, Boolean narrowArticle, Boolean firstConnection, IPrincipal principal) throws JSONException {
        // Update the user
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(principal.getName());
        if (email != null) {
            user.setEmail(email);
        }
        if (themeId != null) {
            user.setTheme(themeId);
        }
        if (localeId != null) {
            user.setLocaleId(localeId);
        }
        if (displayTitleWeb != null) {
            user.setDisplayTitleWeb(displayTitleWeb);
        }
        if (displayTitleMobile != null) {
            user.setDisplayTitleMobile(displayTitleMobile);
        }
        if (displayUnreadWeb != null) {
            user.setDisplayUnreadWeb(displayUnreadWeb);
        }
        if (displayUnreadMobile != null) {
            user.setDisplayUnreadMobile(displayUnreadMobile);
        }
        if (narrowArticle != null) {
            user.setNarrowArticle(narrowArticle);
        }
        if (firstConnection != null && authService.hasBaseFunction(BaseFunction.ADMIN)) {
            user.setFirstConnection(firstConnection);
        }

        user = userDao.update(user);

        if (StringUtils.isNotBlank(password)) {
            user.setPassword(password);
            user = userDao.updatePassword(user);
        }

        if (StringUtils.isNotBlank(password)) {
            // Raise a password updated event
            PasswordChangedEvent passwordChangedEvent = new PasswordChangedEvent();
            passwordChangedEvent.setUser(user);
            AppContext.getInstance().getEventBusManager().getMailEventBus().post(passwordChangedEvent);
        }

        // Always return "ok"
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return response;
    }

    public JSONObject updateUserInfo(String username, String password, String email, String themeId, String localeId, Boolean displayTitleWeb, Boolean displayTitleMobile, Boolean displayUnreadWeb, Boolean displayUnreadMobile, Boolean narrowArticle) throws JSONException {
        // Check if the user exists
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        if (user == null) {
            throw new ClientException("UserNotFound", "The user doesn't exist");
        }

        // Update the user
        if (email != null) {
            user.setEmail(email);
        }
        if (themeId != null) {
            user.setTheme(themeId);
        }
        if (localeId != null) {
            user.setLocaleId(localeId);
        }
        if (displayTitleWeb != null) {
            user.setDisplayTitleWeb(displayTitleWeb);
        }
        if (displayTitleMobile != null) {
            user.setDisplayTitleMobile(displayTitleMobile);
        }
        if (displayUnreadWeb != null) {
            user.setDisplayUnreadWeb(displayUnreadWeb);
        }
        if (displayUnreadMobile != null) {
            user.setDisplayUnreadMobile(displayUnreadMobile);
        }
        if (narrowArticle != null) {
            user.setNarrowArticle(narrowArticle);
        }

        user = userDao.update(user);

        if (StringUtils.isNotBlank(password)) {
            authService.checkBaseFunction(BaseFunction.PASSWORD);

            // Change the password
            user.setPassword(password);
            user = userDao.updatePassword(user);

            // Raise a password updated event
            PasswordChangedEvent passwordChangedEvent = new PasswordChangedEvent();
            passwordChangedEvent.setUser(user);
            AppContext.getInstance().getEventBusManager().getMailEventBus().post(passwordChangedEvent);
        }

        // Always return "ok"
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return response;
    }


    public JSONObject deleteUser(String username) throws JSONException {
        // Check if the user exists
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        if (user == null) {
            throw new ClientException("UserNotFound", "The user doesn't exist");
        }

        // Ensure that the admin user is not deleted
        RoleBaseFunctionDao userBaseFuction = new RoleBaseFunctionDao();
        Set<String> baseFunctionSet = userBaseFuction.findByRoleId(user.getRoleId());
        if (baseFunctionSet.contains(BaseFunction.ADMIN.name())) {
            throw new ClientException("ForbiddenError", "The admin user cannot be deleted");
        }

        // Delete the user
        userDao.delete(user.getUsername());

        // Always return ok
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return response;
    }


}
