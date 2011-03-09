package com.qcadoo.plugin;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.qcadoo.plugin.dependency.PluginDependencyInformation;
import com.qcadoo.plugin.dependency.PluginDependencyResult;

public class PluginManagerUpdateTest {

    private Plugin plugin = mock(Plugin.class);

    private Plugin anotherPlugin = mock(Plugin.class);

    private PluginAccessor pluginAccessor = mock(PluginAccessor.class);

    private PluginDao pluginDao = mock(PluginDao.class);

    private PluginDependencyManager pluginDependencyManager = mock(PluginDependencyManager.class);

    private PluginFileManager pluginFileManager = mock(PluginFileManager.class);

    private PluginServerManager pluginServerManager = mock(PluginServerManager.class);

    private PluginDescriptorParser pluginDescriptorParser = mock(PluginDescriptorParser.class);

    private PluginDependencyInformation pluginInformation = new PluginDependencyInformation("unknownplugin");

    private PluginArtifact pluginArtifact = mock(PluginArtifact.class);

    private DefaultPluginManager pluginManager;

    private File file = mock(File.class);

    @Before
    public void init() {
        given(pluginAccessor.getPlugin("pluginname")).willReturn(plugin);

        given(plugin.getIdentifier()).willReturn("pluginname");

        pluginManager = new DefaultPluginManager();
        pluginManager.setPluginAccessor(pluginAccessor);
        pluginManager.setPluginDao(pluginDao);
        pluginManager.setPluginDependencyManager(pluginDependencyManager);
        pluginManager.setPluginFileManager(pluginFileManager);
        pluginManager.setPluginServerManager(pluginServerManager);
        pluginManager.setPluginDescriptorParser(pluginDescriptorParser);
    }

    @Test
    public void shouldUpdateTemporaryPlugin() throws Exception {
        // given
        given(anotherPlugin.getIdentifier()).willReturn("pluginname");
        given(plugin.hasState(PluginState.TEMPORARY)).willReturn(true);
        given(plugin.getPluginState()).willReturn(PluginState.TEMPORARY);
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        given(plugin.getFilename()).willReturn("filename");

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.satisfiedDependencies();
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(anotherPlugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao).save(anotherPlugin);
        verify(anotherPlugin).changeStateTo(plugin.getPluginState());
        verify(pluginFileManager).uninstallPlugin("filename");
        assertTrue(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.SUCCESS, pluginOperationResult.getStatus());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
    }

    @Test
    public void shouldFailureWithCorruptedPluginOnUpdate() throws Exception {
        // given
        given(pluginDescriptorParser.parse(file)).willThrow(new PluginException());
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);
        given(file.getName()).willReturn("filename");

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao, never()).save(Mockito.any(Plugin.class));
        verify(pluginFileManager).removePlugin("filename");
        assertFalse(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.CORRUPTED_PLUGIN, pluginOperationResult.getStatus());
    }

    @Test
    public void shouldFailureOnUploadingPluginOnUpdate() throws Exception {
        // given
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willThrow(new PluginException());

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao, never()).save(Mockito.any(Plugin.class));
        assertFalse(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.CANNOT_UPLOAD_PLUGIN, pluginOperationResult.getStatus());
    }

    @Test
    public void shouldUpdateTemporaryPluginAndNotifyAboutMissingDependencies() throws Exception {
        // given
        given(anotherPlugin.getIdentifier()).willReturn("pluginname");
        given(plugin.hasState(PluginState.TEMPORARY)).willReturn(true);
        given(plugin.getPluginState()).willReturn(PluginState.TEMPORARY);
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        given(plugin.getFilename()).willReturn("filename");

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.unsatisfiedDependencies(Collections
                .singleton(new PluginDependencyInformation("unknownplugin", null, false, null, false)));
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(anotherPlugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao).save(anotherPlugin);
        verify(anotherPlugin).changeStateTo(plugin.getPluginState());
        verify(pluginFileManager).uninstallPlugin("filename");
        assertTrue(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.SUCCESS_WITH_MISSING_DEPENDENCIES, pluginOperationResult.getStatus());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
        assertTrue(pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies()
                .contains(new PluginDependencyInformation("unknownplugin", null, false, null, false)));
    }

    @Test
    public void shouldUpdateDisabledPlugin() throws Exception {
        // given
        given(anotherPlugin.getIdentifier()).willReturn("pluginname");
        given(plugin.hasState(PluginState.DISABLED)).willReturn(true);
        given(plugin.getPluginState()).willReturn(PluginState.DISABLED);
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        given(plugin.getFilename()).willReturn("filename");
        given(anotherPlugin.getFilename()).willReturn("anotherFilename");
        given(pluginFileManager.installPlugin("anotherFilename")).willReturn(true);

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.satisfiedDependencies();
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(anotherPlugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao).save(anotherPlugin);
        verify(anotherPlugin).changeStateTo(plugin.getPluginState());
        verify(pluginFileManager).uninstallPlugin("filename");
        verify(pluginServerManager).restart();
        assertTrue(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.SUCCESS_WITH_RESTART, pluginOperationResult.getStatus());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
    }

    @Test
    public void shouldFailureUpdateDisabledPluginWithMissingDependencies() throws Exception {
        // given
        given(anotherPlugin.getIdentifier()).willReturn("pluginname");
        given(plugin.hasState(PluginState.DISABLED)).willReturn(true);
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        given(anotherPlugin.getFilename()).willReturn("filename");

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.unsatisfiedDependencies(Collections
                .singleton(new PluginDependencyInformation("unknownplugin", null, false, null, false)));
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(anotherPlugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao, never()).save(anotherPlugin);
        verify(anotherPlugin, never()).changeStateTo(plugin.getPluginState());
        verify(pluginFileManager).removePlugin("filename");
        assertFalse(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.UNSATISFIED_DEPENDENCIES, pluginOperationResult.getStatus());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
        assertTrue(pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies()
                .contains(new PluginDependencyInformation("unknownplugin", null, false, null, false)));
    }

    @Test
    public void shouldUpdateEnabledPlugin() throws Exception {
        // given
        given(anotherPlugin.getIdentifier()).willReturn("pluginname");
        given(plugin.hasState(PluginState.ENABLED)).willReturn(true);
        given(plugin.getPluginState()).willReturn(PluginState.ENABLED);
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        given(plugin.getFilename()).willReturn("filename");

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.satisfiedDependencies();
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(anotherPlugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao).save(anotherPlugin);
        verify(anotherPlugin).changeStateTo(plugin.getPluginState());
        verify(pluginFileManager).uninstallPlugin("filename");
        assertTrue(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.SUCCESS, pluginOperationResult.getStatus());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getDisabledDependencies().size());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
    }

    @Test
    public void shouldFailureUpdateEnabledPluginWithUnsitisfiedDependencies() throws Exception {
        // given
        given(anotherPlugin.getIdentifier()).willReturn("pluginname");
        given(plugin.hasState(PluginState.ENABLED)).willReturn(true);
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        given(anotherPlugin.getFilename()).willReturn("filename");

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.unsatisfiedDependencies(Collections
                .singleton(new PluginDependencyInformation("unknownplugin", null, false, null, false)));
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(anotherPlugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao, never()).save(anotherPlugin);
        verify(anotherPlugin, never()).changeStateTo(plugin.getPluginState());
        verify(pluginFileManager).removePlugin("filename");
        assertFalse(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.UNSATISFIED_DEPENDENCIES, pluginOperationResult.getStatus());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
        assertTrue(pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies()
                .contains(new PluginDependencyInformation("unknownplugin", null, false, null, false)));
    }

    @Test
    public void shouldFailureUpdateEnabledPluginWithDisabledDependencies() throws Exception {
        // given
        given(anotherPlugin.getIdentifier()).willReturn("pluginname");
        given(plugin.hasState(PluginState.ENABLED)).willReturn(true);
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        given(anotherPlugin.getFilename()).willReturn("filename");

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.disabledDependencies(Collections
                .singleton(new PluginDependencyInformation("unknownplugin", null, false, null, false)));
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(anotherPlugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao, never()).save(anotherPlugin);
        verify(anotherPlugin, never()).changeStateTo(plugin.getPluginState());
        verify(pluginFileManager).removePlugin("filename");
        assertFalse(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.DISABLED_DEPENDENCIES, pluginOperationResult.getStatus());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getDisabledDependencies().size());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getDisabledDependencies().size());
        assertTrue(pluginOperationResult.getPluginDependencyResult().getDisabledDependencies()
                .contains(new PluginDependencyInformation("unknownplugin", null, false, null, false)));
    }

    @Test
    public void shouldNotUpdatePluginIfCannotInstall() throws Exception {
        // given
        given(anotherPlugin.getIdentifier()).willReturn("pluginname");
        given(plugin.hasState(PluginState.DISABLED)).willReturn(true);
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        given(plugin.getFilename()).willReturn("filename");
        given(anotherPlugin.getFilename()).willReturn("anotherFilename");
        given(pluginFileManager.installPlugin("anotherFilename")).willReturn(false);

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.satisfiedDependencies();
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(anotherPlugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao, never()).save(anotherPlugin);
        verify(anotherPlugin, never()).changeStateTo(plugin.getPluginState());
        verify(pluginFileManager, never()).removePlugin("filename");
        verify(pluginFileManager).removePlugin("anotherFilename");
        verify(pluginServerManager, never()).restart();
        assertFalse(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.CANNOT_INSTALL_PLUGIN, pluginOperationResult.getStatus());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
    }

    @Test
    public void shouldNotUpdateSystemPlugin() throws Exception {
        // given
        given(pluginDescriptorParser.parse(file)).willReturn(anotherPlugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);
        given(anotherPlugin.isSystemPlugin()).willReturn(true);
        given(anotherPlugin.getFilename()).willReturn("anotherFilename");

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao, never()).save(anotherPlugin);
        verify(pluginFileManager).removePlugin("anotherFilename");
        verify(pluginServerManager, never()).restart();
        assertFalse(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.SYSTEM_PLUGIN_UPDATING, pluginOperationResult.getStatus());
    }

    @Test
    public void shouldInstallPlugin() throws Exception {
        // given
        given(pluginDescriptorParser.parse(file)).willReturn(plugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult.satisfiedDependencies();
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(plugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao).save(plugin);
        assertTrue(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.SUCCESS, pluginOperationResult.getStatus());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getDisabledDependencies().size());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
    }

    @Test
    public void shouldInstallPluginAndNotifyAboutMissingDependencies() throws Exception {
        // given
        given(pluginDescriptorParser.parse(file)).willReturn(plugin);
        given(pluginFileManager.uploadPlugin(pluginArtifact)).willReturn(file);

        PluginDependencyResult pluginDependencyResult = PluginDependencyResult
                .unsatisfiedDependencies(singleton(pluginInformation));
        given(pluginDependencyManager.getDependenciesToEnable(newArrayList(plugin))).willReturn(pluginDependencyResult);

        // when
        PluginOperationResult pluginOperationResult = pluginManager.installPlugin(pluginArtifact);

        // then
        verify(pluginDao).save(plugin);
        assertTrue(pluginOperationResult.isSuccess());
        assertEquals(PluginOperationStatus.SUCCESS_WITH_MISSING_DEPENDENCIES, pluginOperationResult.getStatus());
        assertEquals(0, pluginOperationResult.getPluginDependencyResult().getDisabledDependencies().size());
        assertEquals(1, pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies().size());
        assertTrue(pluginOperationResult.getPluginDependencyResult().getUnsatisfiedDependencies()
                .contains(new PluginDependencyInformation("unknownplugin")));
    }

}
