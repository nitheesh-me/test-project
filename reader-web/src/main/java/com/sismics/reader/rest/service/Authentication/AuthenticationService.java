package com.sismics.reader.rest.service.Authentication;

import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.security.IPrincipal;
import com.sismics.security.UserPrincipal;
import com.sismics.util.filter.SecurityFilter;
import org.codehaus.jettison.json.JSONException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.security.Principal;
import java.util.Set;

public class AuthenticationService {

    @Context
    protected HttpServletRequest request;

    public AuthenticationService(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    private IPrincipal principal;
    public boolean authenticate() {
        Principal principal = (Principal) request.getAttribute(SecurityFilter.getPrincipalAttribute());
        if (principal instanceof IPrincipal) {
            this.principal = (IPrincipal) principal;
            return !this.principal.isAnonymous();
        } else {
            return false;
        }
    }

    public IPrincipal getPrincipal() {
            if(principal==null){
                authenticate();
            }
            return principal;
    }
    public void checkBaseFunction(BaseFunction baseFunction) throws JSONException {
        if (!hasBaseFunction(baseFunction)) {
            throw new ForbiddenClientException();
        }
    }

    /**
     * Checks if the user has a base function.
     *
     * @param baseFunction Base function to check
     * @return True if the user has the base function
     */
    public boolean hasBaseFunction(BaseFunction baseFunction) throws JSONException {
        if (!(getPrincipal() instanceof UserPrincipal)) {
            return false;
        }
        Set<String> baseFunctionSet = ((UserPrincipal) principal).getBaseFunctionSet();
        return baseFunctionSet != null && baseFunctionSet.contains(baseFunction.name());
    }
}
