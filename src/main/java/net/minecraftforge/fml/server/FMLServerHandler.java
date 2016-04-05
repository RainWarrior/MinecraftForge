/*
 * The FML Forge Mod Loader suite. Copyright (C) 2012 cpw
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.minecraftforge.fml.server;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.*;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.*;
import net.minecraft.command.ServerCommand;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.SaveFormatOld;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.IFMLSidedHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.StartupQuery;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.functions.GenericIterableFactory;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Handles primary communication from hooked code into the system
 *
 * The FML entry point is {@link #beginServerLoading(MinecraftServer)} called from
 * {@link net.minecraft.server.dedicated.DedicatedServer}
 *
 * Obfuscated code should focus on this class and other members of the "server"
 * (or "client") code
 *
 * The actual mod loading is handled at arms length by {@link Loader}
 *
 * It is expected that a similar class will exist for each target environment:
 * Bukkit and Client side.
 *
 * It should not be directly modified.
 *
 * @author cpw
 *
 */
public class FMLServerHandler implements IFMLSidedHandler
{
    /**
     * The singleton
     */
    private static final FMLServerHandler INSTANCE = new FMLServerHandler();

    /**
     * A reference to the server itself
     */
    private MinecraftServer server;

    private static class ServerResourceManager implements IResourceManager
    {
        private final ImmutableMap<String, IResourcePack> packMap;
        private final IMetadataSerializer metadataSerializer;

        public ServerResourceManager(IMetadataSerializer metadataSerializer, List<IResourcePack> resourcePacks)
        {
            this.metadataSerializer = metadataSerializer;
            Map<String, IResourcePack> packMap = Maps.newHashMap();
            for (IResourcePack pack : resourcePacks)
            {
                for (String domain : pack.getResourceDomains())
                {
                    if (packMap.containsKey(domain))
                    {
                        throw new IllegalStateException("2 resourcepacks " + pack.getPackName() + " and " + packMap.get(domain).getPackName() + " have the same domain " + domain);
                    }
                    packMap.put(domain, pack);
                }
            }
            this.packMap = ImmutableMap.copyOf(packMap);
        }

        @Override
        public Set<String> getResourceDomains()
        {
            return packMap.keySet();
        }

        @Override
        public IResource getResource(ResourceLocation location) throws IOException
        {
            checkResourcePath(location);
            IResourcePack pack = packMap.get(location.getResourceDomain());
            ResourceLocation metaLocation = new ResourceLocation(location.getResourceDomain(), location.getResourcePath() + ".mcmeta");
            if (pack != null)
            {
                // TODO: InputStreamLeakedResourceLogger?
                InputStream metaStream = null;
                if(pack.resourceExists(metaLocation))
                {
                    metaStream = pack.getInputStream(metaLocation);
                }
                return new SimpleResource(pack.getPackName(), location, pack.getInputStream(location), metaStream, this.metadataSerializer);
            }
            throw new FileNotFoundException();
        }

        @Override
        public List<IResource> getAllResources(ResourceLocation location) throws IOException
        {
            return ImmutableList.of(getResource(location));
        }

        private void checkResourcePath(ResourceLocation location) throws IOException
        {
            if (location.getResourcePath().contains(".."))
            {
                throw new IOException("Invalid relative path to resource: " + location);
            }
        }
    }

    private static class DefaultServerResourcePack extends DefaultResourcePack
    {
        public DefaultServerResourcePack()
        {
            super(new ResourceIndex()
            {
                @Override
                public File getFile(ResourceLocation location)
                {
                    return null;
                }

                @Override
                public boolean isFileExisting(ResourceLocation location)
                {
                    return false;
                }

                @Override
                public File getPackMcmeta()
                {
                    return null;
                }
            });
        }

        @Override
        public InputStream getPackMcmetaResourceStream() throws FileNotFoundException
        {
            return new ByteArrayInputStream(("{\n" +
                    " \"pack\": {\n"+
                    "  \"description\": \"Default Server resource pack.\",\n"+
                    "  \"pack_format\": 2\n"+
                    " },\n" +
                    " \"language\": {\n" +
                    "  \"en_US\": {\n" +
                    "   \"name\": \"en\",\n" +
                    "   \"region\": \"United States of America\",\n" +
                    "   \"bidirectional\": false\n" +
                    "  }\n" +
                    " }\n" +
                    "}").getBytes(Charsets.UTF_8));
        }
    }
    private final IResourcePack defaultResourcePack = new DefaultServerResourcePack();
    private List<IResourcePack> resourcePacks;
    private IMetadataSerializer metadataSerializer = new IMetadataSerializer();
    private ServerResourceManager serverResourceManager;
    private LanguageManager languageManager;

    private FMLServerHandler()
    {
        if((Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment"))
        {
            resourcePacks = Lists.newArrayList();
        }
        else
        {
            resourcePacks = Lists.newArrayList(defaultResourcePack);
        }
        FMLCommonHandler.instance().beginLoading(this, resourcePacks);
    }
    /**
     * Called to start the whole game off from
     * {@link MinecraftServer#startServer}
     *
     * @param minecraftServer
     */
    @Override
    public void beginServerLoading(MinecraftServer minecraftServer)
    {
        server = minecraftServer;
        Loader.instance().loadMods();
        Loader.instance().preinitializeMods();
    }

    /**
     * Called a bit later on during server initialization to finish loading mods
     */
    @Override
    public void finishServerLoading()
    {
        metadataSerializer.registerMetadataSectionType(new PackMetadataSectionSerializer(), PackMetadataSection.class);
        metadataSerializer.registerMetadataSectionType(new LanguageMetadataSectionSerializer(), LanguageMetadataSection.class);
        serverResourceManager = new ServerResourceManager(metadataSerializer, resourcePacks);
        languageManager = new LanguageManager(metadataSerializer, "en_US"); // TODO: switch languages?
        languageManager.onResourceManagerReload(serverResourceManager);
        languageManager.parseLanguageMetadata(resourcePacks);
        Loader.instance().initializeMods();
    }

    @Override
    public void haltGame(String message, Throwable exception)
    {
        throw new RuntimeException(message, exception);
    }

    @Override
    public File getSavesDirectory()
    {
        return ((SaveFormatOld) server.getActiveAnvilConverter()).savesDirectory;
    }

    /**
     * Get the server instance
     */
    @Override
    public MinecraftServer getServer()
    {
        return server;
    }

    @Override
    public IResourceManager getResourceManager()
    {
        return serverResourceManager;
    }

    @Override
    public LanguageManager getLanguageManager()
    {
        return languageManager;
    }

    /**
     * @return the instance
     */
    public static FMLServerHandler instance()
    {
        return INSTANCE;
    }

    /* (non-Javadoc)
     * @see net.minecraftforge.fml.common.IFMLSidedHandler#getAdditionalBrandingInformation()
     */
    @Override
    public List<String> getAdditionalBrandingInformation()
    {
        return ImmutableList.<String>of();
    }

    /* (non-Javadoc)
     * @see net.minecraftforge.fml.common.IFMLSidedHandler#getSide()
     */
    @Override
    public Side getSide()
    {
        return Side.SERVER;
    }

    @Override
    public void showGuiScreen(Object clientGuiElement)
    {

    }

    @Override
    public void queryUser(StartupQuery query) throws InterruptedException
    {
        if (query.getResult() == null)
        {
            FMLLog.warning("%s", query.getText());
            query.finish();
        }
        else
        {
            String text = query.getText() +
                    "\n\nRun the command /fml confirm or or /fml cancel to proceed." +
                    "\nAlternatively start the server with -Dfml.queryResult=confirm or -Dfml.queryResult=cancel to preselect the answer.";
            FMLLog.warning("%s", text);

            if (!query.isSynchronous()) return; // no-op until mc does commands in another thread (if ever)

            boolean done = false;

            while (!done && server.isServerRunning())
            {
                if (Thread.interrupted()) throw new InterruptedException();

                DedicatedServer dedServer = (DedicatedServer) server;

                // rudimentary command processing, check for fml confirm/cancel and stop commands
                synchronized (dedServer.pendingCommandList)
                {
                    for (Iterator<ServerCommand> it = GenericIterableFactory.newCastingIterable(dedServer.pendingCommandList, ServerCommand.class).iterator(); it.hasNext(); )
                    {
                        String cmd = it.next().command.trim().toLowerCase();

                        if (cmd.equals("/fml confirm"))
                        {
                            FMLLog.info("confirmed");
                            query.setResult(true);
                            done = true;
                            it.remove();
                        }
                        else if (cmd.equals("/fml cancel"))
                        {
                            FMLLog.info("cancelled");
                            query.setResult(false);
                            done = true;
                            it.remove();
                        }
                        else if (cmd.equals("/stop"))
                        {
                            StartupQuery.abort();
                        }
                    }
                }

                Thread.sleep(10L);
            }

            query.finish();
        }
    }

    @Override
    public boolean shouldServerShouldBeKilledQuietly()
    {
        return false;
    }

    @Override
    public String getCurrentLanguage()
    {
        return "en_US";
    }

    @Override
    public void serverStopped()
    {
        // NOOP
    }
    @Override
    public NetworkManager getClientToServerNetworkManager()
    {
        throw new RuntimeException("Missing");
    }
    @Override
    public INetHandler getClientPlayHandler()
    {
        return null;
    }

    @Override
    public void fireNetRegistrationEvent(EventBus bus, NetworkManager manager, Set<String> channelSet, String channel, Side side)
    {
        bus.post(new FMLNetworkEvent.CustomPacketRegistrationEvent<NetHandlerPlayServer>(manager, channelSet, channel, side, NetHandlerPlayServer.class));
    }

    @Override
    public boolean shouldAllowPlayerLogins()
    {
        return DedicatedServer.allowPlayerLogins;
    }

    @Override
    public void allowLogins() {
        DedicatedServer.allowPlayerLogins = true;
    }

    @Override
    public IThreadListener getWorldThread(INetHandler net)
    {
        // Always the server on the dedicated server, eventually add Per-World if Mojang adds world stuff.
        return getServer();
    }

    @Override
    public void processWindowMessages()
    {
        // NOOP
    }

    @Override
    public String stripSpecialChars(String message)
    {
        return message;
    }
}
