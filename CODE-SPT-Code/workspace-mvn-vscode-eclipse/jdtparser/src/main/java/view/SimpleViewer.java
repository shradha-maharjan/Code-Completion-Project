/**
 * @file SimpleViewer.java
 */
package view;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import util.SWTResourceManager;

/**
 * @since JavaSE-1.8
 */
public class SimpleViewer {

   private StyledText styledText;

   public SimpleViewer() {
   }

   @PostConstruct
   public void createControls(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(new FillLayout(SWT.HORIZONTAL));
      styledText = new StyledText(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
      styledText.setFont(SWTResourceManager.getFont("Fixedsys", 12, SWT.NORMAL));
   }

   public StyledText getStyledText() {
      return styledText;
   }

   public void appendText(String s) {
      this.styledText.append(s);
   }

   public void reset() {
      this.styledText.setText("");
   }

   @PreDestroy
   public void dispose() {
   }

   @Focus
   public void setFocus() {
      this.styledText.setFocus();
   }

}
