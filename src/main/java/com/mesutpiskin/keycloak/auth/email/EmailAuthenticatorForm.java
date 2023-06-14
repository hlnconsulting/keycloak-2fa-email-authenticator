package com.mesutpiskin.keycloak.auth.email;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.credential.OTPCredentialProviderFactory;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class EmailAuthenticatorForm implements Authenticator
{
    static final String ID = "demo-email-code-form";

    public static final String EMAIL_CODE = "emailCode";

    private final KeycloakSession session;

    public EmailAuthenticatorForm(final KeycloakSession session)
    {
        this.session = session;
    }

    @Override
    public void authenticate(final AuthenticationFlowContext context)
    {
        challenge(context, null);
    }

    private void challenge(final AuthenticationFlowContext context, final FormMessage errorMessage)
    {
        generateAndSendEmailCode(context);

        final LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        if (errorMessage != null)
            form.setErrors(List.of(errorMessage));

        context.challenge(form.createForm("email-code-form.ftl"));
    }

    private void generateAndSendEmailCode(final AuthenticationFlowContext context)
    {

        if (context.getAuthenticationSession().getAuthNote(EMAIL_CODE) != null)
        {
            // skip sending email code
            return;
        }

        final int emailCode = ThreadLocalRandom.current().nextInt(99999999);
        sendEmailWithCode(context.getRealm(), context.getUser(), String.valueOf(emailCode));
        context.getAuthenticationSession().setAuthNote(EMAIL_CODE, Integer.toString(emailCode));
    }

    @Override
    public void action(final AuthenticationFlowContext context)
    {
        final MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("resend"))
        {
            resetEmailCode(context);
            challenge(context, null);
            return;
        }

        if (formData.containsKey("cancel"))
        {
            resetEmailCode(context);
            context.resetFlow();
            return;
        }

        boolean valid;
        try
        {
            valid = validateCode(context, Integer.parseInt(formData.getFirst(EMAIL_CODE)));
        }
        catch (final NumberFormatException e)
        {
            valid = false;
        }

        if (!valid)
        {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            challenge(context, new FormMessage(Messages.INVALID_ACCESS_CODE));
            return;
        }

        resetEmailCode(context);
        context.success();
    }

    private void resetEmailCode(final AuthenticationFlowContext context)
    {
        context.getAuthenticationSession().removeAuthNote(EMAIL_CODE);
    }

    private boolean validateCode(final AuthenticationFlowContext context, final int givenCode)
    {
        return givenCode == Integer.parseInt(context.getAuthenticationSession().getAuthNote(EMAIL_CODE));
    }

    @Override
    public boolean requiresUser()
    {
        return true;
    }

    @Override
    public boolean configuredFor(final KeycloakSession session, final RealmModel realm, final UserModel user)
    {
        return !user.credentialManager().isConfiguredFor(getCredentialProvider(session).getType());
    }

    public OTPCredentialProvider getCredentialProvider(final KeycloakSession session)
    {
        return (OTPCredentialProvider) session.getProvider(CredentialProvider.class, OTPCredentialProviderFactory.PROVIDER_ID);
    }

    @Override
    public void setRequiredActions(final KeycloakSession session, final RealmModel realm, final UserModel user)
    {
        // NOOP
    }

    @Override
    public void close()
    {
        // NOOP
    }

    private void sendEmailWithCode(final RealmModel realm, final UserModel user, final String code)
    {
        if (user.getEmail() == null)
        {
            log.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(), user.getUsername());
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER);
        }

        final Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("username", user.getUsername());
        mailBodyAttributes.put("code", code);

        final List<Object> subjectParams = List.of(realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());

        try
        {
            final EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            // Don't forget to add the welcome-email.ftl (html and text) template to your theme.
            emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
        }
        catch (final EmailException eex)
        {
            log.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
        }
    }
}
