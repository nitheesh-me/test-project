package com.sismics.reader.rest.service.User;

import com.sismics.reader.core.constant.Constants;
import com.sismics.reader.core.dao.jpa.JobDao;
import com.sismics.reader.core.dao.jpa.JobEventDao;
import com.sismics.reader.core.dao.jpa.UserDao;
import com.sismics.reader.core.dao.jpa.criteria.JobCriteria;
import com.sismics.reader.core.dao.jpa.criteria.JobEventCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserCriteria;
import com.sismics.reader.core.dao.jpa.dto.JobDto;
import com.sismics.reader.core.dao.jpa.dto.JobEventDto;
import com.sismics.reader.core.dao.jpa.dto.UserDto;
import com.sismics.reader.core.model.jpa.User;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.core.util.jpa.SortCriteria;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.reader.rest.service.Authentication.AuthencticationService;
import com.sismics.security.IPrincipal;
import com.sismics.security.UserPrincipal;
import com.sismics.util.LocaleUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.List;

public class UserInfoService {
    private final AuthencticationService authService;
    private final UserDao userDao;
    private final JobDao jobDao;
    private final JobEventDao jobEventDao;
    private final JobCriteria jobCriteria;

    public UserInfoService(@Context HttpServletRequest request) {
        this.authService = new AuthencticationService(request);
        this.userDao = new UserDao();
        this.jobDao = new JobDao();
        this.jobEventDao = new JobEventDao();
        this.jobCriteria = new JobCriteria();
    }

    public JSONObject getInfo(IPrincipal principal, HttpServletRequest request) throws JSONException {
        JSONObject response = new JSONObject();
        if (!authService.authenticate()) {
            response.put("anonymous", true);

            String localeId = LocaleUtil.getLocaleIdFromAcceptLanguage(request.getHeader("Accept-Language"));
            response.put("locale", localeId);

            // Check if admin has the default password
            User adminUser = userDao.getById("admin");
            if (adminUser != null && adminUser.getDeleteDate() == null) {
                response.put("is_default_password", Constants.DEFAULT_ADMIN_PASSWORD.equals(adminUser.getPassword()));
            }
        } else {
            response.put("anonymous", false);
            getUserData(principal, response);
            List<JobDto> jobList = jobDao.findByCriteria(jobCriteria);
            JSONArray jobs = new JSONArray();

            getJobData(jobList, jobs, response);
        }
        return response;
    }

    private void getJobData(List<JobDto> jobList, JSONArray jobs, JSONObject response) throws JSONException {
        for (JobDto job : jobList) {
            JSONObject jobJson = new JSONObject();
            jobJson.put("id", job.getId());
            jobJson.put("name", job.getName());
            jobJson.put("start_date", job.getStartTimestamp());
            jobJson.put("end_date", job.getStartTimestamp());

            JobEventCriteria jobEventCriteria = new JobEventCriteria()
                    .setJobId(job.getId());
            List<JobEventDto> jobEventList = jobEventDao.findByCriteria(jobEventCriteria);
            int feedSuccess = 0;
            int feedFailure = 0;
            int starredSuccess = 0;
            int starredFailure = 0;
            for (JobEventDto jobEvent : jobEventList) {
                String name = jobEvent.getName();
                switch (name) {
                    case Constants.JOB_EVENT_FEED_COUNT:
                    jobJson.put("feed_total", Integer.valueOf(jobEvent.getValue()));
                        break;
                    case Constants.JOB_EVENT_STARRED_ARTICLED_COUNT:
                    jobJson.put("starred_total", Integer.valueOf(jobEvent.getValue()));
                        break;
                    case Constants.JOB_EVENT_FEED_IMPORT_SUCCESS:
                    feedSuccess++;
                        break;
                    case Constants.JOB_EVENT_FEED_IMPORT_FAILURE:
                    feedFailure++;
                        break;
                    case Constants.JOB_EVENT_STARRED_ARTICLE_IMPORT_SUCCESS:
                    starredSuccess++;
                        break;
                    case Constants.JOB_EVENT_STARRED_ARTICLE_IMPORT_FAILURE:
                    starredFailure++;
                        break;
                }
            }
            jobJson.put("feed_success", Integer.valueOf(feedSuccess));
            jobJson.put("feed_failure", Integer.valueOf(feedFailure));
            jobJson.put("starred_success", Integer.valueOf(starredSuccess));
            jobJson.put("starred_failure", Integer.valueOf(starredFailure));
            jobs.put(jobJson);
        }
        response.put("jobs", jobs);
    }

    private void getUserData(IPrincipal principal, JSONObject response) throws JSONException {
        User user = userDao.getById(principal.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("theme", user.getTheme());
        response.put("locale", user.getLocaleId());
        response.put("display_title_web", user.isDisplayTitleWeb());
        response.put("display_title_mobile", user.isDisplayTitleMobile());
        response.put("display_unread_web", user.isDisplayUnreadWeb());
        response.put("display_unread_mobile", user.isDisplayUnreadMobile());
        response.put("narrow_article", user.isNarrowArticle());
        response.put("first_connection", user.isFirstConnection());
        JSONArray baseFunctions = new JSONArray(((UserPrincipal) principal).getBaseFunctionSet());
        response.put("base_functions", baseFunctions);
        response.put("is_default_password", authService.hasBaseFunction(BaseFunction.ADMIN) && Constants.DEFAULT_ADMIN_PASSWORD.equals(user.getPassword()));


        jobCriteria.setUserId(user.getId());
    }


    public JSONObject getAllUsers(Integer limit, Integer offset, Integer sortColumn, Boolean asc) throws JSONException {
        JSONObject response = new JSONObject();
        List<JSONObject> users = new ArrayList<JSONObject>();

        PaginatedList<UserDto> paginatedList = PaginatedLists.create(limit, offset);
        SortCriteria sortCriteria = new SortCriteria(sortColumn, asc);

        userDao.findByCriteria(paginatedList, new UserCriteria(), sortCriteria, null);
        for (UserDto userDto : paginatedList.getResultList()) {
            JSONObject user = new JSONObject();
            user.put("id", userDto.getId());
            user.put("username", userDto.getUsername());
            user.put("email", userDto.getEmail());
            user.put("create_date", userDto.getCreateTimestamp());
            users.add(user);
        }
        response.put("total", paginatedList.getResultCount());
        response.put("users", users);
        return response;
    }
}
