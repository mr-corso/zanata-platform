package org.zanata.webtrans.client.view;

import org.zanata.webtrans.client.presenter.ValidationOptionsPresenter;
import org.zanata.webtrans.client.resources.WebTransMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ValidationOptionsView extends Composite implements ValidationOptionsPresenter.Display
{

   private static ValidationOptionsViewUiBinder uiBinder = GWT.create(ValidationOptionsViewUiBinder.class);

   interface ValidationOptionsViewUiBinder extends UiBinder<Widget, ValidationOptionsView>
   {
   }

   @UiField
   Label validationOptionsHeader;

   @UiField
   VerticalPanel contentPanel;
   
   @UiField
   PushButton runValidation;


   @Inject
   public ValidationOptionsView(WebTransMessages messages)
   {
      initWidget(uiBinder.createAndBindUi(this));
      validationOptionsHeader.setText(messages.validationOptions());
      runValidation.setText("Run validation");
   }

   @Override
   public HasValueChangeHandlers<Boolean> addValidationSelector(String label, String tooltip, boolean enabled, boolean locked)
   {
      CheckBox chk = new CheckBox(label);
      chk.setValue(enabled);
      chk.setTitle(tooltip);
      chk.setEnabled(!locked);
      contentPanel.add(chk);

      return chk;
   }

   @Override
   public void changeValidationSelectorValue(String label, boolean enabled)
   {
      for (Widget checkbox : contentPanel)
      {
         if (checkbox instanceof CheckBox && ((CheckBox) checkbox).getText().equals(label))
         {
            ((CheckBox) checkbox).setValue(enabled);
         }
      }
   }

   @Override
   public Widget asWidget()
   {
      return this;
   }

   @Override
   public void clearValidationSelector()
   {
      contentPanel.clear();
   }

   @Override
   public HasClickHandlers getRunValidationButton()
   {
      return runValidation;
   }
}
