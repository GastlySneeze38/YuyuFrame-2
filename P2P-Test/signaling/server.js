import { WebSocketServer } from 'ws';

const PORT = 8765;
const peers = new Map(); // uuid → { ws, name, x, z }

const wss = new WebSocketServer({ port: PORT });
console.log(`[Signaling] ws://localhost:${PORT}`);

wss.on('connection', (ws) => {
  let myId = null;

  ws.on('message', (raw) => {
    const msg = JSON.parse(raw);

    if (msg.type === 'join') {
      myId = msg.id;
      peers.set(myId, { ws, name: msg.name, x: 0, z: 0 });
      console.log(`+ ${msg.name} (${peers.size} connectés)`);

      // Envoyer les peers existants
      ws.send(JSON.stringify({
        type: 'peer_list',
        peers: [...peers.entries()]
          .filter(([id]) => id !== myId)
          .map(([id, p]) => ({ id, name: p.name, x: p.x, z: p.z }))
      }));

      broadcast(myId, { type: 'peer_joined', id: myId, name: msg.name, x: 0, z: 0 });
    }

    if (msg.type === 'position') {
      const p = peers.get(myId);
      if (p) { p.x = msg.x; p.z = msg.z; }
      broadcast(myId, { type: 'position', id: myId, x: msg.x, z: msg.z });
    }
  });

  ws.on('close', () => {
    if (!myId) return;
    const p = peers.get(myId);
    peers.delete(myId);
    console.log(`- ${p?.name} (${peers.size} connectés)`);
    broadcast(myId, { type: 'peer_left', id: myId });
  });
});

function broadcast(senderId, msg) {
  const data = JSON.stringify(msg);
  for (const [id, p] of peers) {
    if (id !== senderId && p.ws.readyState === 1) p.ws.send(data);
  }
}
