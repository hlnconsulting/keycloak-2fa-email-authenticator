package com.mesutpiskin.keycloak.auth.email;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import com.google.auto.service.AutoService;

@AutoService(AuthenticatorFactory.class)
public class EmailAuthenticatorFormFactory implements AuthenticatorFactory
{
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES =
            { AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                    AuthenticationExecutionModel.Requirement.DISABLED };

    @Override
    public String getDisplayType()
    {
        return "Email OTP";
    }

    @Override
    public String getReferenceCategory()
    {
        return null;
    }

    @Override
    public boolean isConfigurable()
    {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices()
    {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed()
    {
        return false;
    }

    @Override
    public String getHelpText()
    {
        return "Email otp authenticator.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return null;
    }

    @Override
    public void close()
    {
        // NOOP
    }

    @Override
    public Authenticator create(final KeycloakSession session)
    {
        return new EmailAuthenticatorForm(session);
    }

    @Override
    public void init(final Config.Scope config)
    {
        // NOOP
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory)
    {
        // NOOP
    }

    @Override
    public String getId()
    {
        return EmailAuthenticatorForm.ID;
    }
}
