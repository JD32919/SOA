package com.example.ventas.modelo;

import java.util.List;
import java.util.UUID;

public record ReservaRemota(UUID operacionId, String estado, List<LineaReservaRemota> lineas) {}
