package com.publiccms.views.controller.admin.cms;

import static com.sanluan.common.tools.RequestUtils.getIpAddress;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.publiccms.common.base.AbstractController;
import com.publiccms.entities.cms.CmsTagType;
import com.publiccms.entities.log.LogOperate;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.logic.service.cms.CmsTagTypeService;
import com.publiccms.logic.service.log.LogLoginService;

@Controller
@RequestMapping("cmsTagType")
public class CmsTagTypeController extends AbstractController {
    @Autowired
    private CmsTagTypeService service;

    @RequestMapping(SAVE)
    public String save(CmsTagType entity, HttpServletRequest request, HttpSession session, ModelMap model) {
        SysSite site = getSite(request);
        if (notEmpty(entity.getId())) {
            CmsTagType oldEntity = service.getEntity(entity.getId());
            if (empty(oldEntity) || virifyNotEquals("siteId", site.getId(), oldEntity.getSiteId(), model)) {
                return TEMPLATE_ERROR;
            }
            service.update(entity.getId(), entity, new String[] { ID, "siteId" });
            if (notEmpty(entity.getId())) {
                logOperateService.save(new LogOperate(site.getId(), getAdminFromSession(session).getId(),
                        LogLoginService.CHANNEL_WEB_MANAGER, "update.tagType", getIpAddress(request), getDate(), entity.getId()
                                + ":" + entity.getName()));
            }
        } else {
            entity.setSiteId(site.getId());
            service.save(entity);
            logOperateService.save(new LogOperate(site.getId(), getAdminFromSession(session).getId(),
                    LogLoginService.CHANNEL_WEB_MANAGER, "save.tagType", getIpAddress(request), getDate(), entity.getId() + ":"
                            + entity.getName()));
        }
        return TEMPLATE_DONE;
    }

    @RequestMapping(DELETE)
    public String delete(Integer id, HttpServletRequest request, HttpSession session, ModelMap model) {
        SysSite site = getSite(request);
        CmsTagType entity = service.getEntity(id);
        if (notEmpty(entity)) {
            if (virifyNotEquals("siteId", site.getId(), entity.getSiteId(), model)) {
                return TEMPLATE_ERROR;
            }
            service.delete(id);
            logOperateService.save(new LogOperate(site.getId(), getAdminFromSession(session).getId(),
                    LogLoginService.CHANNEL_WEB_MANAGER, "delete.tagType", getIpAddress(request), getDate(), id.toString()));
        }
        return TEMPLATE_DONE;
    }
}