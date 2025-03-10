package io.metersphere.functional.service;

import io.metersphere.functional.constants.MinderLabel;
import io.metersphere.functional.domain.FunctionalCase;
import io.metersphere.functional.domain.FunctionalCaseCustomField;
import io.metersphere.functional.dto.FunctionalCaseMindDTO;
import io.metersphere.functional.dto.FunctionalCaseStepDTO;
import io.metersphere.functional.dto.FunctionalMinderTreeDTO;
import io.metersphere.functional.dto.FunctionalMinderTreeNodeDTO;
import io.metersphere.functional.mapper.ExtFunctionalCaseMapper;
import io.metersphere.functional.mapper.FunctionalCaseCustomFieldMapper;
import io.metersphere.functional.mapper.FunctionalCaseMapper;
import io.metersphere.functional.request.FunctionalCaseMindRequest;
import io.metersphere.functional.request.FunctionalCaseMinderEditRequest;
import io.metersphere.sdk.util.JSON;
import io.metersphere.system.domain.CustomField;
import io.metersphere.system.domain.CustomFieldExample;
import io.metersphere.system.mapper.CustomFieldMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能用例脑图
 *
 * @date : 2023-5-17
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class FunctionalCaseMinderService {

    @Resource
    private ExtFunctionalCaseMapper extFunctionalCaseMapper;

    @Resource
    private FunctionalCaseMapper functionalCaseMapper;

    @Resource
    private CustomFieldMapper customFieldMapper;

    @Resource
    private FunctionalCaseCustomFieldMapper functionalCaseCustomFieldMapper;

    /**
     * 功能用例-脑图用例列表查询
     *
     * @param deleted 用例是否删除
     * @return FunctionalMinderTreeDTO
     */
    public List<FunctionalMinderTreeDTO> getMindFunctionalCase(FunctionalCaseMindRequest request, boolean deleted) {
        List<FunctionalMinderTreeDTO> list = new ArrayList<>();
        //查出当前模块下的所有用例
        List<FunctionalCaseMindDTO> functionalCaseMindDTOList = extFunctionalCaseMapper.getMinderCaseList(request, deleted);
        //构造父子级数据
        for (FunctionalCaseMindDTO functionalCaseMindDTO : functionalCaseMindDTOList) {
            FunctionalMinderTreeDTO root = new FunctionalMinderTreeDTO();
            FunctionalMinderTreeNodeDTO rootData = new FunctionalMinderTreeNodeDTO();
            rootData.setId(functionalCaseMindDTO.getId());
            rootData.setPos(functionalCaseMindDTO.getPos());
            rootData.setText(functionalCaseMindDTO.getName());
            rootData.setPriority(functionalCaseMindDTO.getPriority());
            rootData.setStatus(functionalCaseMindDTO.getReviewStatus());
            rootData.setResource(List.of(MinderLabel.CASE.toString(), functionalCaseMindDTO.getPriority()));
            List<FunctionalMinderTreeDTO> children = buildChildren(functionalCaseMindDTO);
            root.setChildren(children);
            root.setData(rootData);
            list.add(root);
        }
        return list;
    }

    private List<FunctionalMinderTreeDTO> buildChildren(FunctionalCaseMindDTO functionalCaseMindDTO) {
        List<FunctionalMinderTreeDTO> children = new ArrayList<>();
        if (functionalCaseMindDTO.getTextDescription() != null) {
            String textDescription = new String(functionalCaseMindDTO.getTextDescription(), StandardCharsets.UTF_8);
            FunctionalMinderTreeDTO stepFunctionalMinderTreeDTO = getFunctionalMinderTreeDTO(textDescription, MinderLabel.TEXT_DESCRIPTION.toString(), 0L);
            if (functionalCaseMindDTO.getExpectedResult() != null) {
                String expectedResultText = new String(functionalCaseMindDTO.getExpectedResult(), StandardCharsets.UTF_8);
                FunctionalMinderTreeDTO expectedResultFunctionalMinderTreeDTO = getFunctionalMinderTreeDTO(expectedResultText, MinderLabel.EXPECTED_RESULT.toString(), 0L);
                stepFunctionalMinderTreeDTO.getChildren().add(expectedResultFunctionalMinderTreeDTO);
            }
            children.add(stepFunctionalMinderTreeDTO);
        }

        if (functionalCaseMindDTO.getSteps() != null) {
            String stepText = new String(functionalCaseMindDTO.getSteps(), StandardCharsets.UTF_8);
            List<FunctionalCaseStepDTO> functionalCaseStepDTOS = JSON.parseArray(stepText, FunctionalCaseStepDTO.class);
            for (FunctionalCaseStepDTO functionalCaseStepDTO : functionalCaseStepDTOS) {
                FunctionalMinderTreeDTO stepFunctionalMinderTreeDTO = getFunctionalMinderTreeDTO(functionalCaseStepDTO.getDesc(), MinderLabel.STEPS.toString(), Long.valueOf(functionalCaseStepDTO.getNum()));
                if (functionalCaseMindDTO.getExpectedResult() != null) {
                    FunctionalMinderTreeDTO expectedResultFunctionalMinderTreeDTO = getFunctionalMinderTreeDTO(functionalCaseStepDTO.getResult(), MinderLabel.EXPECTED_RESULT.toString(), 0L);
                    stepFunctionalMinderTreeDTO.getChildren().add(expectedResultFunctionalMinderTreeDTO);
                }
                children.add(stepFunctionalMinderTreeDTO);
            }
        }

        if (functionalCaseMindDTO.getPrerequisite() != null) {
            String prerequisiteText = new String(functionalCaseMindDTO.getPrerequisite(), StandardCharsets.UTF_8);
            FunctionalMinderTreeDTO prerequisiteFunctionalMinderTreeDTO = getFunctionalMinderTreeDTO(prerequisiteText, MinderLabel.PREREQUISITE.toString(), 0L);
            children.add(prerequisiteFunctionalMinderTreeDTO);
        }
        if (functionalCaseMindDTO.getDescription() != null) {
            String descriptionText = new String(functionalCaseMindDTO.getDescription(), StandardCharsets.UTF_8);
            FunctionalMinderTreeDTO descriptionFunctionalMinderTreeDTO = getFunctionalMinderTreeDTO(descriptionText, MinderLabel.DESCRIPTION.toString(), 0L);
            children.add(descriptionFunctionalMinderTreeDTO);
        }
        return children;
    }

    @NotNull
    private static FunctionalMinderTreeDTO getFunctionalMinderTreeDTO(String text, String resource, Long pos) {
        FunctionalMinderTreeDTO functionalMinderTreeDTO = new FunctionalMinderTreeDTO();
        FunctionalMinderTreeNodeDTO rootData = new FunctionalMinderTreeNodeDTO();
        rootData.setText(text);
        rootData.setPos(pos);
        rootData.setResource(List.of(resource));
        functionalMinderTreeDTO.setChildren(new ArrayList<>());
        functionalMinderTreeDTO.setData(rootData);
        return functionalMinderTreeDTO;
    }

    public FunctionalCase updateFunctionalCase(FunctionalCaseMinderEditRequest request, String userId) {
        if (StringUtils.isNotBlank(request.getName())) {
            FunctionalCase functionalCase = new FunctionalCase();
            functionalCase.setName(request.getName());
            buildUpdateCaseParam(request, userId, functionalCase);
            functionalCaseMapper.updateByPrimaryKeySelective(functionalCase);
            return functionalCase;
        }
        if (StringUtils.isNotBlank(request.getPriority())) {
            CustomFieldExample example = new CustomFieldExample();
            example.createCriteria().andNameEqualTo("functional_priority").andSceneEqualTo("FUNCTIONAL").andScopeIdEqualTo(request.getProjectId());
            List<CustomField> customFields = customFieldMapper.selectByExample(example);
            String field = customFields.get(0).getId();
            FunctionalCaseCustomField customField = new FunctionalCaseCustomField();
            customField.setCaseId(request.getId());
            customField.setFieldId(field);
            customField.setValue(request.getPriority());
            functionalCaseCustomFieldMapper.updateByPrimaryKeySelective(customField);
            FunctionalCase functionalCase = new FunctionalCase();
            buildUpdateCaseParam(request, userId, functionalCase);
            functionalCaseMapper.updateByPrimaryKeySelective(functionalCase);
            return functionalCase;
        }
        return new FunctionalCase();
    }

    private static void buildUpdateCaseParam(FunctionalCaseMinderEditRequest request, String userId, FunctionalCase functionalCase) {
        functionalCase.setId(request.getId());
        functionalCase.setUpdateUser(userId);
        functionalCase.setUpdateTime(System.currentTimeMillis());
    }
}
