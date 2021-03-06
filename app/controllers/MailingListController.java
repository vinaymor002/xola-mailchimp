package controllers;

import controllers.helpers.MailingListHelper;
import daos.InstallationDao;
import models.Installation;
import org.springframework.util.StringUtils;
import play.Logger;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.AccessLoggingAction;
import utils.ErrorUtil;
import utils.MessageKey;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author rishabh
 */
@With(AccessLoggingAction.class)
public class MailingListController extends Controller {

    private final Logger.ALogger log = Logger.of(MailingListController.class);

    private final InstallationDao installationDao;
    private final MessagesApi messagesApi;
    private final FormFactory formFactory;
    private final MailingListHelper helper;

    @Inject
    public MailingListController(InstallationDao installationDao, MessagesApi messagesApi, FormFactory formFactory,
                                 MailingListHelper helper) {
        this.installationDao = installationDao;
        this.messagesApi = messagesApi;
        this.formFactory = formFactory;
        this.helper = helper;
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public CompletionStage<Result> getLists() {
        log.info("Request made to retrieve the mailings lists... as json");
        Messages messages = messagesApi.preferred(request());
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String installationId = requestData.get("iid");
        if (!StringUtils.hasText(installationId)) {
            log.debug("Error validating the request parameters... Missing installation id");
            return CompletableFuture.completedFuture(invalidParameters(messages, "iid", MessageKey.INVALID_PARAM_IID));
        }
        String key = requestData.get("key");
        if (!StringUtils.hasText(key)) {
            log.debug("Error validating the request parameters... Missing field key");
            return CompletableFuture.completedFuture(invalidParameters(messages, "key", MessageKey.INVALID_PARAM_KEY));
        }
        log.info("Requesting mailing list for installation {}", installationId);
        Optional<Installation> installation = installationDao.getByInstallationId(installationId);
        if (installation.isPresent()) {
            return helper.getMailingListsAsJson(installation.get(), messages);
        } else {
            log.debug("Didn't find the installation object having id: {}", installationId);
            return CompletableFuture.completedFuture(
                    notFound(ErrorUtil.toJson(NOT_FOUND, messages.at(MessageKey.NOT_FOUND))));
        }
    }

    private Result invalidParameters(Messages messages, String parameterName, String messageKey) {
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put(parameterName, messages.at(messageKey));
        return badRequest(ErrorUtil.toJson(BAD_REQUEST, messages.at(MessageKey.VALIDATION_ERRORS), errorMap));
    }

}
