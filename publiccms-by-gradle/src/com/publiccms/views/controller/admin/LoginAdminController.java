package com.publiccms.views.controller.admin;

import static com.publiccms.logic.service.log.LogLoginService.CHANNEL_WEB_MANAGER;
import static com.sanluan.common.tools.RequestUtils.getIpAddress;
import static com.sanluan.common.tools.VerificationUtils.encode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.publiccms.common.base.AbstractController;
import com.publiccms.entities.log.LogLogin;
import com.publiccms.entities.log.LogOperate;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.entities.sys.SysUser;
import com.publiccms.logic.component.CacheComponent;
import com.publiccms.logic.service.log.LogLoginService;
import com.publiccms.logic.service.sys.SysUserService;

@Controller
public class LoginAdminController extends AbstractController {
    @Autowired
    private SysUserService service;
    @Autowired
    private LogLoginService logLoginService;
    @Autowired
    private CacheComponent cacheComponent;

    @RequestMapping(value = "login", method = RequestMethod.POST)
    public String login(String username, String password, String returnUrl, HttpServletRequest request, HttpSession session,
            HttpServletResponse response, ModelMap model) {
        SysSite site = getSite(request);
        if (virifyNotEmpty("username", username, model) || virifyNotEmpty("password", password, model)) {
            model.addAttribute("username", username);
            model.addAttribute("returnUrl", returnUrl);
            return "login";
        }
        String ip = getIpAddress(request);
        SysUser user = service.findByName(site.getId(), username);
        if (virifyNotExist("username", user, model) || virifyNotEquals("password", encode(password), user.getPassword(), model)
                || virifyNotAdmin(user, model)) {
            model.addAttribute("username", username);
            model.addAttribute("returnUrl", returnUrl);
            Integer userId = null;
            if (notEmpty(user)) {
                userId = user.getId();
            }
            logLoginService
                    .save(new LogLogin(site.getId(), username, userId, ip, CHANNEL_WEB_MANAGER, false, getDate(), password));
            return "login";
        }

        setAdminToSession(session, user);
        service.updateLoginStatus(user.getId(), getIpAddress(request));
        logLoginService.save(new LogLogin(site.getId(), username, user.getId(), ip, CHANNEL_WEB_MANAGER, true, getDate(), null));

        if (notEmpty(returnUrl)) {
            try {
                returnUrl = URLDecoder.decode(returnUrl, DEFAULT_CHARSET);
                return REDIRECT + returnUrl;
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage());
                return REDIRECT + "index.html";
            }
        } else {
            return REDIRECT + "index.html";
        }
    }

    @RequestMapping(value = "loginDialog", method = RequestMethod.POST)
    public String loginDialog(String username, String password, HttpServletRequest request, HttpSession session,
            HttpServletResponse response, ModelMap model) {
        login(username, password, null, request, session, response, model);
        return TEMPLATE_DONE;
    }

    @RequestMapping(value = "changePassword", method = RequestMethod.POST)
    public String changeMyselfPassword(Integer id, String oldpassword, String password, String repassword,
            HttpServletRequest request, HttpSession session, ModelMap model) {
        SysSite site = getSite(request);
        SysUser user = getAdminFromSession(session);
        if (virifyNotEquals("siteId", site.getId(), user.getSiteId(), model)) {
            return TEMPLATE_ERROR;
        }
        if (virifyNotEquals("password", user.getPassword(), encode(oldpassword), model)) {
            return TEMPLATE_ERROR;
        } else if (virifyNotEmpty("password", password, model) || virifyNotEquals("repassword", password, repassword, model)) {
            return TEMPLATE_ERROR;
        } else {
            clearAdminToSession(session);
            model.addAttribute(MESSAGE, "message.needReLogin");
        }
        service.updatePassword(user.getId(), encode(password));
        logOperateService.save(new LogOperate(site.getId(), user.getId(), LogLoginService.CHANNEL_WEB_MANAGER, "changepassword",
                getIpAddress(request), getDate(), user.getPassword()));
        return "common/ajaxTimeout";
    }

    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public String logout(HttpSession session) {
        clearAdminToSession(session);
        return REDIRECT + "index.html";
    }

    @RequestMapping(value = "clearCache")
    public String clearCache(HttpServletRequest request) {
        cacheComponent.clear();
        return TEMPLATE_DONE;
    }

    protected boolean virifyNotAdmin(SysUser user, ModelMap model) {
        if (!user.isDisabled() && !user.isSuperuserAccess()) {
            model.addAttribute(ERROR, "verify.user.notAdmin");
            return true;
        }
        return false;
    }
}