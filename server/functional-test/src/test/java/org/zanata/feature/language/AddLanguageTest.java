/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.zanata.feature.language;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zanata.feature.Trace;
import org.zanata.feature.testharness.TestPlan.DetailedTest;
import org.zanata.feature.testharness.ZanataTestCase;
import org.zanata.page.administration.AddLanguagePage;
import org.zanata.page.languages.LanguagesPage;
import org.zanata.page.projects.projectsettings.ProjectLanguagesTab;
import org.zanata.workflow.BasicWorkFlow;
import org.zanata.workflow.LoginWorkFlow;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Category(DetailedTest.class)
public class AddLanguageTest extends ZanataTestCase {

    @Before
    public void before() {
        new BasicWorkFlow().goToHome().deleteCookiesAndRefresh();
        assertThat(new LoginWorkFlow().signIn("admin", "admin").loggedInAs())
                .as("Admin is logged in")
                .isEqualTo("admin");
    }

    @Trace(summary = "The administrator can add a language to Zanata")
    @Test(timeout = ZanataTestCase.MAX_SHORT_TEST_DURATION)
    public void addLanguageAsEnabledByDefault() throws Exception {
        String language = "Goa'uld";
        LanguagesPage languagesPage = new BasicWorkFlow()
            .goToHome()
            .goToLanguages();

        assertThat(languagesPage.getLanguageLocales())
                .as("The language is not listed")
                .doesNotContain(language);

        languagesPage = languagesPage
                .clickAddNewLanguage()
                .enterSearchLanguage(language)
                .enterLanguageName("Goa'uld")
                .enterLanguageNativeName("Kek mattet")
                .enterLanguagePlurals("nplurals=2; plural=(n != 1)")
                .expectPluralsWarning()
                .saveLanguage();

        assertThat(languagesPage.getLanguageLocales())
                .as("The language is listed")
                .contains(language);

        assertThat(languagesPage.languageIsEnabledByDefault(language))
                .as("The language is enabled by default")
                .isTrue();

        languagesPage.closeNotification();

        List<String> enabledLocaleList = languagesPage
                .goToHomePage()
                .gotoExplore()
                .searchAndGotoProjectByName("about fedora")
                .gotoSettingsTab()
                .gotoSettingsLanguagesTab()
                .getEnabledLocaleList();

        assertThat(enabledLocaleList)
                .as("The language is enabled by default")
                .contains(language);
    }

    @Trace(summary = "The administrator can add a disabled language to Zanata")
    @Test(timeout = ZanataTestCase.MAX_SHORT_TEST_DURATION)
    public void addLanguageAsDisabledByDefault() throws Exception {
        String language = "Klingon";
        LanguagesPage languagesPage = new BasicWorkFlow()
                .goToHome()
                .goToHomePage()
                .goToLanguages();

        assertThat(languagesPage.getLanguageLocales())
                .as("The language is not listed")
                .doesNotContain(language);

        languagesPage = languagesPage
                .clickAddNewLanguage()
                .enterSearchLanguage(language)
                .expectPluralsWarning()
                .enterLanguageName("ta' Hol")
                .enterLanguageNativeName("tlhIngan")
                .enterLanguagePlurals("nplurals=2; plural=(n != 1)")
                .disableLanguageByDefault()
                .saveLanguage();

        assertThat(languagesPage.getLanguageLocales())
                .as("The language is listed")
                .contains(language);
        assertThat(languagesPage.languageIsEnabledByDefault(language))
                .as("The language is disabled by default")
                .isFalse();

        languagesPage.closeNotification();

        ProjectLanguagesTab projectLanguagesTab = languagesPage.goToHomePage()
                .gotoExplore()
                .searchAndGotoProjectByName("about fedora")
                .gotoSettingsTab()
                .gotoSettingsLanguagesTab();
        List<String> enabledLocaleList =
                projectLanguagesTab.getEnabledLocaleList();

        assertThat(enabledLocaleList)
                .as("The language is disabled by default")
                .doesNotContain(language);
    }

    @Trace(summary = "The administrator can add a known language to Zanata")
    @Test(timeout = ZanataTestCase.MAX_SHORT_TEST_DURATION)
    public void addKnownLanguage() throws Exception {
        String language = "ru-RU";
        LanguagesPage languagesPage = new BasicWorkFlow()
                .goToHome()
                .goToLanguages();

        assertThat(languagesPage.getLanguageLocales())
                .as("The language is not listed")
                .doesNotContain(language);

        AddLanguagePage addLanguagePage = languagesPage
                .clickAddNewLanguage()
                .enterSearchLanguage(language)
                .selectSearchLanguage(language);

        assertThat(addLanguagePage.getNewLanguageName())
                .as("The name is correct")
                .isEqualTo("Russian (Russia)");
        assertThat(addLanguagePage.getNewLanguageNativeName())
                .as("The native name is correct")
                .isEqualTo("русский (Россия)");
        assertThat(addLanguagePage.getNewLanguageCode())
                .as("The language is correct")
                .isEqualTo(language);
    }

}
