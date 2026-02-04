package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.model.ActionToken;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.ActionTokenService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActionTokenHandler implements UpdateHandler {

    private final ActionTokenService actionTokenService;

    @Override
    public boolean supports(UpdateHandlerContext value) {
        return value.update().hasCallbackQuery();
    }

    @Override
    public void handle(UpdateHandlerContext value) {
        String callbackData = value.update().getCallbackQuery().getData();

        ActionToken token = actionTokenService.decode(callbackData);

        switch (token.action()) {

        }
    }
}
