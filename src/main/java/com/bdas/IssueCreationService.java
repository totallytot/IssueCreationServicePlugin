package com.bdas;

import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.service.AbstractService;
import com.atlassian.jira.user.ApplicationUser;
import com.opensymphony.module.propertyset.PropertySet;

public class IssueCreationService extends AbstractService {
    private String projectKey, reporter, assignee, summary, issueTypeID, description, priotityID;
    private Project project = null;

    @Override
    public void init(PropertySet props) throws ObjectConfigurationException {
        super.init(props);

        if (hasProperty("Project Key")) projectKey = getProperty("Project Key");
        if (hasProperty("Reporter")) reporter = getProperty("Reporter");
        if (hasProperty("Assignee")) assignee = getProperty("Assignee");

        if (hasProperty("Summary")) summary = getProperty("Summary");
        else summary = "Summary for issue generated from service. Can be set from Administration => System => Services.";

        if (hasProperty("Description")) description = getProperty("Description");
        else description = "Description for issue generated from service. Can be set from Administration => System => Services.";

        if (hasProperty("Issue Type ID")) issueTypeID = getProperty("Issue Type ID");
        else issueTypeID = "10000";

        if (hasProperty("Priority ID")) priotityID = getProperty("Priority ID");
        else priotityID = "3";
    }

    @Override
    public void run() {
        if (projectKey != null || reporter != null) {

            project = ComponentAccessor.getProjectManager().getProjectByCurrentKeyIgnoreCase(projectKey);

            IssueService issueService = ComponentAccessor.getIssueService();
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
            ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(reporter);

            //required for service
            JiraAuthenticationContext jiraAuthenticationContextReporter = ComponentAccessor.getJiraAuthenticationContext();
            jiraAuthenticationContextReporter.setLoggedInUser(applicationUser);

            if (applicationUser != null) {
                issueInputParameters.setProjectId(project.getId())
                        .setReporterId(applicationUser.getUsername())
                        .setPriorityId(priotityID)
                        .setSummary(summary)
                        .setIssueTypeId(issueTypeID)
                        .setDescription(description)
                        .setSkipScreenCheck(true);

                IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(applicationUser, issueInputParameters);
                if (createValidationResult.isValid()) {
                    IssueService.IssueResult createResult = issueService.create(applicationUser, createValidationResult);

                    if (createResult.isValid() && assignee != null)
                    {
                        IssueService.AssignValidationResult assignValidationResult = issueService.validateAssign(applicationUser, createResult.getIssue().getId(), assignee);

                        if (assignValidationResult.isValid()) issueService.assign(applicationUser, assignValidationResult);
                    }
                }
            }
        }
    }

    public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException {
        return getObjectConfiguration("IssueCreationService",
                "IssueCreationService.xml", null);
    }
}
