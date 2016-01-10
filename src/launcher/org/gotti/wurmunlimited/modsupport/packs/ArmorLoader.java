package org.gotti.wurmunlimited.modsupport.packs;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ModClient;

import com.wurmonline.client.renderer.cell.ArmorMeshData;
import com.wurmonline.client.renderer.cell.ArmorXmlParser;
import com.wurmonline.client.resources.ResourceUrl;
import com.wurmonline.shared.xml.XmlNode;
import com.wurmonline.shared.xml.XmlParser;

public class ArmorLoader {

	private static Logger logger;

	static {
		logger = Logger.getLogger(ArmorLoader.class.getName());

	}

	public void load(ResourceUrl armor) {

		if (ModClient.getWorld().getArmorMeshDataList() == null) {
			ModClient.getWorld().setArmorMeshDataList(ArmorXmlParser.parse());
		}
		
		XmlNode rootNode = null;

		try (final InputStream input = armor.openStream()) {
			rootNode = XmlParser.parse(input);
		}

		catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		if (rootNode != null) {
			
			Map<String, ArmorMeshData> armorData = ModClient.getWorld().getArmorMeshDataList();

			final List<XmlNode> categorys = (List<XmlNode>) rootNode.getChildren();

			for (final XmlNode categoryNode : categorys) {

				final List<XmlNode> items = (List<XmlNode>) categoryNode.getChildren();

				for (final XmlNode itemNode : items) {

					final String itemName = itemNode.getName();

					final List<XmlNode> kingdom = (List<XmlNode>) itemNode.getChildren();

					for (final XmlNode kingdomNode : kingdom) {

						final String kingdomName = kingdomNode.getName();
						final List<XmlNode> genders = (List<XmlNode>) kingdomNode.getChildren();

						for (final XmlNode genderNode : genders) {

							final String genderName = genderNode.getName();

							final String[] meshPlaces = getValueFromXmlTag(genderNode, "meshPlace");
							final String[] meshNames = getValueFromXmlTag(genderNode, "meshName");

							if (meshPlaces.length != meshNames.length) {
							}

							final String[] texturePlaces = getValueFromXmlTag(genderNode, "texturePlace");
							final String[] meshTextureNames = getValueFromXmlTag(genderNode, "texture");

							if (texturePlaces.length != meshTextureNames.length) {
							}

							final boolean[] textureHasAlpha = new boolean[meshTextureNames.length];
							for (int i = 0; i < textureHasAlpha.length; ++i) {
								textureHasAlpha[i] = meshTextureNames[i].contains("hasAlpha");

							}

							boolean boolean1 = genderNode.getFirst("isSkirt") != null;
							boolean1 = (genderNode.getFirst("showHair") != null || boolean1);
							boolean1 = (genderNode.getFirst("isBelt") != null || boolean1);
							boolean1 = (genderNode.getFirst("isWeapon") != null || boolean1);

							boolean boolean2 = genderNode.getFirst("removeHead") != null;
							boolean2 = (genderNode.getFirst("isTwoHander") != null || boolean2);

							final boolean boolean3 = genderNode.getFirst("removeMask") == null;

							final ArmorMeshData itemData = new ArmorMeshData(itemName, meshPlaces, meshNames, texturePlaces, meshTextureNames, textureHasAlpha, boolean1, boolean2, boolean3);
							armorData.put(String.valueOf(itemName) + "_" + kingdomName + "_" + genderName, itemData);
							
							for (int i = 0; i < meshTextureNames.length; i++) {
								ModArmor.addArmorTexture(meshTextureNames[i], armor);
							}
						}
					}
				}
			}
		}
	}

	private static String[] getValueFromXmlTag(final XmlNode xmlNode, final String xmlTag) {
		final List<XmlNode> childeren = (List<XmlNode>) xmlNode.getAll(xmlTag);
		final String[] valueArray = new String[childeren.size()];

		for (int i = 0; i < childeren.size(); ++i) {

			final String text = childeren.get(i).getText();
			valueArray[i] = text;

		}
		return valueArray;
	}

}
