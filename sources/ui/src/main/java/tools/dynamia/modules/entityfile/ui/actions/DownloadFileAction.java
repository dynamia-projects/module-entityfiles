package tools.dynamia.modules.entityfile.ui.actions;

import tools.dynamia.actions.ActionGroup;
import tools.dynamia.actions.InstallAction;
import tools.dynamia.modules.entityfile.StoredEntityFile;
import tools.dynamia.modules.entityfile.domain.EntityFile;
import tools.dynamia.modules.entityfile.ui.util.EntityFileUtils;
import tools.dynamia.ui.MessageType;
import tools.dynamia.ui.UIMessages;

@InstallAction
public class DownloadFileAction extends AbstractEntityFileAction {

	public DownloadFileAction() {
		setName("Descargar Archivo");
		setImage("icons:download");
		setGroup(ActionGroup.get("FILES"));
		setMenuSupported(true);
	}

	@Override
	public void actionPerformed(EntityFileActionEvent evt) {

		EntityFile file = evt.getEntityFile();
		if (file != null) {

			StoredEntityFile sef = file.getStoredEntityFile();
			if (sef != null && sef.getUrl() != null) {
				EntityFileUtils.showDownloadDialog(sef);
			} else {
				UIMessages.showMessage("No se pudo encontrar archivo " + file.getName()
						+ " en el servidor, por favor contacte con el administrador del sistema", MessageType.ERROR);
			}

		} else {
			UIMessages.showMessage("Seleccion archivo para descargar", MessageType.WARNING);
		}

	}

}
