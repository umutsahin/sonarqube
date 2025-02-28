/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.rule;

import com.google.common.collect.ImmutableSet;
import java.util.Date;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.sonar.api.rule.RuleKey.EXTERNAL_RULE_REPO_PREFIX;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;

/**
 * Utility class for tests involving rules
 */
public class RuleTesting {

  public static final RuleKey EXTERNAL_XOO = RuleKey.of(EXTERNAL_RULE_REPO_PREFIX + "xoo", "x1");
  public static final RuleKey XOO_X1 = RuleKey.of("xoo", "x1");
  public static final RuleKey XOO_X2 = RuleKey.of("xoo", "x2");
  public static final RuleKey XOO_X3 = RuleKey.of("xoo", "x3");

  private static final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private RuleTesting() {
    // only static helpers
  }

  public static RuleDto newRule() {
    return newRule(randomRuleKey());
  }

  public static RuleDto newRule(RuleKey key) {
    RuleDto ruleDto = newRuleWithoutDescriptionSection(key);
    ruleDto.addRuleDescriptionSectionDto(createDefaultRuleDescriptionSection(uuidFactory.create(), "description_" + randomAlphabetic(5)));
    return ruleDto;
  }

  public static RuleDto newRuleWithoutDescriptionSection() {
    return newRuleWithoutDescriptionSection(randomRuleKey());
  }

  public static RuleDto newRuleWithoutDescriptionSection(RuleKey ruleKey) {
    long currentTimeMillis = System.currentTimeMillis();
    return new RuleDto()
      .setRepositoryKey(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setUuid("rule_uuid_" + randomAlphanumeric(5))
      .setName("name_" + randomAlphanumeric(5))
      .setDescriptionFormat(RuleDto.Format.HTML)
      .setType(RuleType.values()[nextInt(RuleType.values().length)])
      .setStatus(RuleStatus.READY)
      .setConfigKey("configKey_" + randomAlphanumeric(5))
      .setSeverity(Severity.ALL.get(nextInt(Severity.ALL.size())))
      .setIsTemplate(false)
      .setIsExternal(false)
      .setIsAdHoc(false)
      .setSystemTags(newHashSet("tag_" + randomAlphanumeric(5), "tag_" + randomAlphanumeric(5)))
      .setLanguage("lang_" + randomAlphanumeric(3))
      .setGapDescription("gapDescription_" + randomAlphanumeric(5))
      .setDefRemediationBaseEffort(nextInt(10) + "h")
      //voluntarily offset the remediation to be able to detect issues
      .setDefRemediationGapMultiplier((nextInt(10) + 10) + "h")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setCreatedAt(currentTimeMillis)
      .setUpdatedAt(currentTimeMillis + 5)
      .setScope(Scope.MAIN);
  }

  public static RuleMetadataDto newRuleMetadata() {
    return new RuleMetadataDto()
      .setRuleUuid("uuid_" + randomAlphanumeric(5))
      .setRemediationBaseEffort(nextInt(10) + "h")
      .setRemediationGapMultiplier(nextInt(10) + "h")
      .setRemediationFunction("LINEAR_OFFSET")
      .setTags(newHashSet("tag_" + randomAlphanumeric(5), "tag_" + randomAlphanumeric(5)))
      .setNoteData("noteData_" + randomAlphanumeric(5))
      .setNoteUserUuid("noteUserUuid_" + randomAlphanumeric(5))
      .setNoteCreatedAt(System.currentTimeMillis() - 200)
      .setNoteUpdatedAt(System.currentTimeMillis() - 150)
      .setAdHocName("adHocName_" + randomAlphanumeric(5))
      .setAdHocDescription("adHocDescription_" + randomAlphanumeric(5))
      .setAdHocSeverity(Severity.ALL.get(nextInt(Severity.ALL.size())))
      .setAdHocType(RuleType.values()[nextInt(RuleType.values().length - 1)]);
  }

  public static RuleMetadataDto newRuleMetadata(RuleDto rule) {
    return newRuleMetadata()
      .setRuleUuid(rule.getUuid());
  }

  public static RuleMetadataDto newRuleMetadata(RuleDto rule, UserDto noteUser) {
    return newRuleMetadata(rule).setNoteUserUuid(noteUser.getUuid());
  }

  public static RuleParamDto newRuleParam(RuleDto rule) {
    return new RuleParamDto()
      .setRuleUuid(rule.getUuid())
      .setName("name_" + randomAlphabetic(5))
      .setDefaultValue("default_" + randomAlphabetic(5))
      .setDescription("description_" + randomAlphabetic(5))
      .setType(RuleParamType.STRING.type());
  }

  public static DeprecatedRuleKeyDto newDeprecatedRuleKey() {
    return new DeprecatedRuleKeyDto()
      .setUuid(uuidFactory.create())
      .setOldRepositoryKey(randomAlphanumeric(50))
      .setOldRuleKey(randomAlphanumeric(50))
      .setRuleUuid(randomAlphanumeric(40))
      .setCreatedAt(System.currentTimeMillis());
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX1() {
    return newDto(XOO_X1).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX2() {
    return newDto(XOO_X2).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX3() {
    return newDto(XOO_X3).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newDto(RuleKey ruleKey) {

    return new RuleDto()
      .setUuid("uuid_" + Uuids.createFast())
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescriptionFormat(RuleDto.Format.HTML)
      .addRuleDescriptionSectionDto(createDefaultRuleDescriptionSection(uuidFactory.create(), "Description" + ruleKey.rule()))
      .setStatus(RuleStatus.READY)
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setIsTemplate(false)
      .setSystemTags(ImmutableSet.of("systag1", "systag2"))
      .setLanguage("js")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix")
      .setType(RuleType.CODE_SMELL)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime())
      .setScope(Scope.MAIN)
      .setTags(ImmutableSet.of("tag1", "tag2"))
      .setRemediationFunction("LINEAR")
      .setRemediationGapMultiplier("1h");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newRuleDto() {
    return newDto(RuleKey.of(randomAlphanumeric(30), randomAlphanumeric(30)));
  }

  public static RuleDto newTemplateRule(RuleKey ruleKey) {
    return newDto(ruleKey)
      .setIsTemplate(true);
  }


  public static RuleDto newCustomRule(RuleDto templateRule) {
    checkNotNull(templateRule.getUuid(), "The template rule need to be persisted before creating this custom rule.");
    return newRule(RuleKey.of(templateRule.getRepositoryKey(), templateRule.getRuleKey() + "_" + System.currentTimeMillis()))
      .setLanguage(templateRule.getLanguage())
      .setTemplateUuid(templateRule.getUuid())
      .setType(templateRule.getType());
  }

  public static RuleKey randomRuleKey() {
    return RuleKey.of("repo_" + randomAlphanumeric(3), "rule_" + randomAlphanumeric(3));
  }

  public static RuleKey randomRuleKeyOfMaximumLength() {
    return RuleKey.of(randomAlphabetic(255), randomAlphabetic(200));
  }

  public static Consumer<RuleDto> setRepositoryKey(String repositoryKey) {
    return rule -> rule.setRepositoryKey(repositoryKey);
  }

  public static Consumer<RuleDto> setCreatedAt(long createdAt) {
    return rule -> rule.setCreatedAt(createdAt);
  }

  public static Consumer<RuleDto> setUpdatedAt(long updatedtAt) {
    return rule -> rule.setUpdatedAt(updatedtAt);
  }

  public static Consumer<RuleDto> setRuleKey(String ruleKey) {
    return rule -> rule.setRuleKey(ruleKey);
  }

  public static Consumer<RuleDto> setName(String name) {
    return rule -> rule.setName(name);
  }

  public static Consumer<RuleDto> setLanguage(String language) {
    return rule -> rule.setLanguage(language);
  }

  public static Consumer<RuleDto> setSeverity(String severity) {
    return rule -> rule.setSeverity(severity);
  }

  public static Consumer<RuleDto> setStatus(RuleStatus status) {
    return rule -> rule.setStatus(status);
  }

  public static Consumer<RuleDto> setType(RuleType type) {
    return rule -> rule.setType(type);
  }

  public static Consumer<RuleDto> setIsExternal(boolean isExternal) {
    return rule -> rule.setIsExternal(isExternal);
  }

  public static Consumer<RuleDto> setSecurityStandards(Set<String> securityStandards) {
    return rule -> rule.setSecurityStandards(securityStandards);
  }

  public static Consumer<RuleDto> setIsTemplate(boolean isTemplate) {
    return rule -> rule.setIsTemplate(isTemplate);
  }

  public static Consumer<RuleDto> setTemplateId(@Nullable String templateUuid) {
    return rule -> rule.setTemplateUuid(templateUuid);
  }

  public static Consumer<RuleDto> setSystemTags(String... tags) {
    return rule -> rule.setSystemTags(copyOf(tags));
  }

  public static Consumer<RuleMetadataDto> setTags(String... tags) {
    return rule -> rule.setTags(copyOf(tags));
  }

}
