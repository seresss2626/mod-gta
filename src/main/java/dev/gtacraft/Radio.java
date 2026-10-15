package dev.gtacraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Радио в машине. Станции играют пластинки из самой игры. */
public final class Radio {
	private record Track(String sound, int seconds) {}

	private record Station(String name, Track[] tracks) {}

	private static final Station[] STATIONS = {
		new Station("Выключено", new Track[0]),
		new Station("Лос-Блокос FM", new Track[]{new Track("blocks", 345), new Track("mall", 197), new Track("cat", 185), new Track("13", 178)}),
		new Station("Нефритовое радио", new Track[]{new Track("chirp", 185), new Track("far", 174), new Track("strad", 188), new Track("wait", 238)}),
		new Station("Подземный рок", new Track[]{new Track("pigstep", 149), new Track("otherside", 195), new Track("relic", 218), new Track("stal", 150)})
	};

	private static final class State {
		int station = 1;
		int track;
		long endsAt;
		boolean playing;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private static State st(ServerPlayer p) {
		return STATES.computeIfAbsent(p.getUUID(), k -> new State());
	}

	public static String stationName(ServerPlayer p) {
		State s = st(p);
		return p.getVehicle() != null && CarManager.isCarEntity(p.getVehicle()) ? STATIONS[s.station].name() : "";
	}

	public static void next(ServerPlayer p) {
		State s = st(p);
		stop(p);
		s.station = (s.station + 1) % STATIONS.length;
		s.track = 0;
		s.endsAt = 0;
		p.displayClientMessage(Component.literal("Радио: " + STATIONS[s.station].name()), true);
	}

	private static void stop(ServerPlayer p) {
		State s = st(p);
		if (s.playing) {
			p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.RECORDS));
			s.playing = false;
		}
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			State s = st(p);
			boolean inCar = p.getVehicle() != null && CarManager.isCarEntity(p.getVehicle());
			if (!inCar || s.station == 0) {
				stop(p);
				s.endsAt = 0;
				continue;
			}
			long now = p.level().getGameTime();
			if (!s.playing || now >= s.endsAt) {
				Track[] tracks = STATIONS[s.station].tracks();
				if (s.playing) {
					s.track = (s.track + 1) % tracks.length;
				}
				Track t = tracks[s.track % tracks.length];
				SoundEvent ev = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("music_disc." + t.sound()));
				p.connection.send(new ClientboundSoundEntityPacket(Holder.direct(ev), SoundSource.RECORDS, p, 1.0f, 1.0f, p.level().getRandom().nextLong()));
				s.playing = true;
				s.endsAt = now + t.seconds() * 20L;
			}
		}
	}
}
