package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.HandlerResult;
import hr.lknezevic.barberbot.enums.SessionState;
import hr.lknezevic.barberbot.handler.Handler;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;

import java.util.EnumSet;

public interface UpdateHandler extends Handler<UpdateHandlerContext, HandlerResult> {
    EnumSet<SessionState> supportedStates();
}
