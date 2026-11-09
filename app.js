"use strict";

const API_CATALOGO = "http://localhost:9001/api";
const API_CLIENTES = "http://localhost:9003/api";

const elemento = (id) => document.getElementById(id);

const ETIQUETAS ={
    id: "ID",
    nombre: "Nombre",
    precio: "Precio",
    existencias: "Existencias",
    activo: "Activo",
    operacionId: "UUID de la operacion",
    estado: "Estado",
    lineas: "Lineas reservadas",
    email: "Email",
    telefono: "Telefono",
    limiteCredito: "Limite de credito",
    habilitado: "Habilitado",
    motivoInhabilitacion: "Motivo de inhabilitacion",
    clienteId: "ID del cliente",
    consumido: "Consumido",
    disponible: "Disponible",
    puedeComprar: "Puede comprar",
    motivo: "Motivo",
    monto: "Monto",
    registradoEn: "Fecha"
};


function mostrarMensaje(id, texto, esError = false){
    const destino = elemento(id);
    destino.textContent = texto;
    destino.className = esError
    ? "mensaje error"
    : "mensaje exito";
}

function mostrarJson(id,datos){
    const destino = elemento(id);
    destino.replaceChildren();

    const tabla = document.createElement("table");

    for (const [clave, valor] of Object.entries(datos)) {
        const fila = document.createElement("tr");

        const celdaClave = document.createElement("th");
        celdaClave.textContent = ETIQUETAS[clave] ?? clave;
        fila.appendChild(celdaClave);

        const celdaValor = document.createElement("td");

        if (Array.isArray(valor)) {
           
            if (valor.length === 0) {
                celdaValor.textContent = "Sin líneas";
            }

            for (const linea of valor) {
                const texto = document.createElement("div");
                texto.textContent =
                    `Producto ${linea.productoId} — cantidad ${linea.cantidad}`;
                celdaValor.appendChild(texto);
            }

        } else if (typeof valor === "boolean") {
            celdaValor.textContent = valor ? "Sí" : "No";

        } else if (clave === "precio" || clave === "limiteCredito" || clave === "consumido" || clave === "disponible" || clave === "monto") {
            celdaValor.textContent = `$${Number(valor).toFixed(2)}`;

        } else {
            celdaValor.textContent = String(valor ?? "");
        }

        fila.appendChild(celdaValor);
        tabla.appendChild(fila);
    }

    destino.appendChild(tabla);
}

function leerEntero(idCampo, minimo, nombre){
    const texto = elemento(idCampo).value.trim();
    const numero = Number(texto);

    if(texto === "" || !Number.isInteger(numero) || numero < minimo || numero > 2147483647){
        throw new Error(`${nombre} debe ser un entero valido (minimo ${minimo}).`);
        
    }
    return numero;
}

function leerPrecio(){
    const texto = elemento("crearPrecio").value.trim();
    const numero = Number(texto);

    if(texto ==="" || !(numero > 0)){
        throw new Error("El precio debe ser mayor que cero.");
        
    }
    return numero;
}

const FORMATO_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function leerUuid(){
    const texto = elemento("reservaUuid").value.trim();
    if(!FORMATO_UUID.test(texto)){
        throw new Error("Escribir un UUID valido o genera uno nuevo.");

    }
    return texto.toLowerCase();

}

function leerLineas(){
    const texto = elemento("reservaLineas").value.trim();

    if(texto === ""){
        throw new Error("Escribe al menos una linea, por ejemplo 1:2.");                
    }
    return texto.split(",").map((parte) => {
        const par = parte.trim().split(":");
        const productoId = Number(par[0]);
        const cantidad = Number(par[1]);

        if(par.length !== 2 || !Number.isInteger(productoId) || !Number.isInteger(cantidad) ||productoId < 1 || cantidad < 1){
            throw new Error("Cada linea debe tener el formato producto:cantidad.");
            
        }
        return {productoId, cantidad};
    });

}

async function solicitarJson(url, opciones = {}){
    const controlador = new AbortController();

    const temporizador = setTimeout(() => {
        controlador.abort();
    }, 20000);

    try {
        const respuesta = await fetch (url, {
            ...opciones,
            signal: controlador.signal
        });

        const texto =await respuesta.text();

        let datos = null;

        if(texto){
            try{datos = JSON.parse(texto);
            } catch {
                datos = null
            }
        }

        if(!respuesta.ok){
            const detalle =
            datos && typeof datos.mensaje === "string"
            ? datos.mensaje
            : `El servicio respondio con HTTP ${respuesta.status}.`;

            throw new Error(detalle);
            
        }

        if(datos ===null){
            throw new Error("El servicio no devolvio un JSON valido.");
            
        }
        return datos;

    } catch (error){
        if(
            error.name === "AbortError" || error instanceof TypeError
        ){
            throw new Error("No fue posible comunicarse con el servicio. " + "Comprueba que este activo.");
            
        }
        throw error;
    }finally{
        clearTimeout(temporizador);
    }
    
}

function enviarJson(url, metodo, cuerpo){
    return solicitarJson(url, {
        method: metodo,
        headers:{
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: JSON.stringify(cuerpo)
    });
}


//catalogo

async function listarProductos(){
    elemento("tablaProductos") .hidden = true;
    elemento("cuerpoProductos").replaceChildren();

    mostrarMensaje("mensajeProductos", "consultando productos...");

    try{const productos = await solicitarJson(`${API_CATALOGO}/productos`);

        if(!Array.isArray(productos)){
            throw new Error("El servicio no devolvio una lista.");
            
        }

        for (const producto of productos){
            const fila = document.createElement("tr");

            const valores = [
                producto.id,
                producto.nombre,
                producto.precio,
                producto.existencias,
                producto.activo ? "Si" : "No"
            ];
            for (const valor of valores){
                const celda = document.createElement("td");
                celda.textContent = String(valor ?? "");
                fila.appendChild(celda);
            }
            elemento("cuerpoProductos").appendChild(fila);
        }

        elemento("tablaProductos").hidden = productos.length === 0;

        mostrarMensaje(
            "mensajeProductos",
            `Productos registrados: ${productos.length}.`

        );
    } catch (error) {
        mostrarMensaje("mensajeProductos", error.message, true);
    }
}

elemento("btnListar").addEventListener("click", listarProductos);

elemento("formConsultaProducto").addEventListener("submit", async (evento) =>{
    evento.preventDefault();

    try{
        const id = leerEntero("consultaId", 1, "El identificador");
        const producto = await solicitarJson(`${API_CATALOGO}/productos/${id}`);

        mostrarMensaje("mensajeProductos", "Consulta realizada correctamente.");
        mostrarJson("salidaProducto", producto);
    } catch(error){
        elemento("salidaProducto").textContent = "";
        mostrarMensaje("mensajeProductos", error.message, true);
    }
});
//crear producto
elemento("formCrear").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    try {
        const cuerpo = {
            nombre: elemento("crearNombre").value.trim(),
            precio: leerPrecio(),
            existencias: leerEntero("crearExistencias", 0, "Las existencias")
        };

         const producto = await enviarJson(
            `${API_CATALOGO}/productos`,
            "POST",
            cuerpo
        );

        mostrarMensaje("mensajeCrear", `Producto ${producto.id} creado.`);
        mostrarJson("salidaCrear", producto);
        await listarProductos();
    } catch (error){
        elemento("salidaCrear").textContent = "";
        mostrarMensaje("mensajeCrear", error.message, true);
    }

});
//cambiar existencias
elemento("formExistencias").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    try {
        const id = leerEntero("existenciasId", 1, "El identificador");
        const existencias = leerEntero(
            "existenciasValor", 0, "Las existencias"
        );

        const producto = await enviarJson(
            `${API_CATALOGO}/productos/${id}/existencias`,
            "PUT",
            { existencias }
        );

        mostrarMensaje("mensajeExistencias", "Existencias actualizadas.");
        mostrarJson("salidaExistencias", producto);
        await listarProductos();

    } catch (error) {
        elemento("salidaExistencias").textContent = "";
        mostrarMensaje("mensajeExistencias", error.message, true);
    }
});

//reservas
elemento("btnNuevoUuid").addEventListener("click", () => {
    elemento("reservaUuid").value = crypto.randomUUID();
});

elemento("formReserva").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    try {
        const uuid = leerUuid();
        const lineas = leerLineas();

        const reserva = await enviarJson(`${API_CATALOGO}/reservas/${uuid}`,
            "PUT",
            { lineas }
        );

        mostrarMensaje("mensajeReserva", `Reserva ${reserva.estado}.`);
        mostrarJson("salidaReserva", reserva);
        await listarProductos();

    } catch (error) {
        elemento("salidaReserva").textContent = "";
        mostrarMensaje("mensajeReserva", error.message, true);
    }
});

// Cancelar
elemento("btnCancelarReserva").addEventListener("click", async () => {
    try {
        const uuid = leerUuid();

        const reserva = await solicitarJson(`${API_CATALOGO}/reservas/${uuid}/cancelacion`,
            { method: "PUT" }
        );

        mostrarMensaje("mensajeReserva", `Reserva ${reserva.estado}.`);
        mostrarJson("salidaReserva", reserva);
        await listarProductos();

    } catch (error) {
        elemento("salidaReserva").textContent = "";
        mostrarMensaje("mensajeReserva", error.message, true);
    }
});

elemento("btnConsultarReserva").addEventListener("click", async () => {
    try {
        const uuid = leerUuid();

        const reserva = await solicitarJson(`${API_CATALOGO}/reservas/${uuid}`);

        mostrarMensaje("mensajeReserva", `Reserva ${reserva.estado}.`);
        mostrarJson("salidaReserva", reserva);

    } catch (error) {
        elemento("salidaReserva").textContent = "";
        mostrarMensaje("mensajeReserva", error.message, true);
    }
});

elemento("reservaUuid").value = crypto.randomUUID();
listarProductos();

//clientes

function leerMonto(idCampo, nombre, permitirCero){
    const texto = elemento(idCampo).value.trim();
    const numero = Number(texto);

    if(texto === "" || Number.isNaN(numero) || numero < 0 || (!permitirCero && numero === 0)){
        throw new Error(`${nombre} debe ser un numero ${permitirCero ? "mayor o igual a cero" : "mayor que cero"}.`);
    }
    return numero;
}

async function listarClientes(){
    elemento("tablaClientes").hidden = true;
    elemento("cuerpoClientes").replaceChildren();

    mostrarMensaje("mensajeClientes", "consultando clientes...");

    try{
        const clientes = await solicitarJson(`${API_CLIENTES}/clientes`);

        if(!Array.isArray(clientes)){
            throw new Error("El servicio no devolvio una lista.");
        }

        for (const cliente of clientes){
            const fila = document.createElement("tr");
            const valores = [
                cliente.id,
                cliente.nombre,
                cliente.email,
                `$${Number(cliente.limiteCredito).toFixed(2)}`,
                cliente.habilitado ? "Si" : "No"
            ];
            for (const valor of valores){
                const celda = document.createElement("td");
                celda.textContent = String(valor ?? "");
                fila.appendChild(celda);
            }
            elemento("cuerpoClientes").appendChild(fila);
        }

        elemento("tablaClientes").hidden = clientes.length === 0;
        mostrarMensaje("mensajeClientes", `Clientes registrados: ${clientes.length}.`);
    } catch (error) {
        mostrarMensaje("mensajeClientes", error.message, true);
    }
}

function limpiarSalidaCliente(){
    elemento("salidaCliente").textContent = "";
    elemento("tablaHistorial").hidden = true;
    elemento("cuerpoHistorial").replaceChildren();
}

elemento("btnListarClientes").addEventListener("click", listarClientes);

elemento("formConsultaCliente").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    limpiarSalidaCliente();

    try{
        const id = leerEntero("clienteConsultaId", 1, "El identificador");
        const cliente = await solicitarJson(`${API_CLIENTES}/clientes/${id}`);

        mostrarMensaje("mensajeClientes", "Consulta realizada correctamente.");
        mostrarJson("salidaCliente", cliente);
    } catch(error){
        mostrarMensaje("mensajeClientes", error.message, true);
    }
});

elemento("btnCreditoCliente").addEventListener("click", async () => {
    limpiarSalidaCliente();

    try{
        const id = leerEntero("clienteConsultaId", 1, "El identificador");
        const credito = await solicitarJson(`${API_CLIENTES}/clientes/${id}/credito`);

        mostrarMensaje("mensajeClientes", `Credito disponible: $${Number(credito.disponible).toFixed(2)}.`);
        mostrarJson("salidaCliente", credito);
    } catch(error){
        mostrarMensaje("mensajeClientes", error.message, true);
    }
});

elemento("btnHistorialCliente").addEventListener("click", async () => {
    limpiarSalidaCliente();

    try{
        const id = leerEntero("clienteConsultaId", 1, "El identificador");
        const historial = await solicitarJson(`${API_CLIENTES}/clientes/${id}/historial`);

        for (const consumo of historial){
            const fila = document.createElement("tr");
            const valores = [
                consumo.operacionId,
                `$${Number(consumo.monto).toFixed(2)}`,
                consumo.estado,
                new Date(consumo.registradoEn).toLocaleString()
            ];
            for (const valor of valores){
                const celda = document.createElement("td");
                celda.textContent = String(valor ?? "");
                fila.appendChild(celda);
            }
            elemento("cuerpoHistorial").appendChild(fila);
        }

        elemento("tablaHistorial").hidden = historial.length === 0;
        mostrarMensaje("mensajeClientes", `Consumos en el historial: ${historial.length}.`);
    } catch(error){
        mostrarMensaje("mensajeClientes", error.message, true);
    }
});

//crear cliente
elemento("formCrearCliente").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    try {
        const cuerpo = {
            nombre: elemento("clienteNombre").value.trim(),
            email: elemento("clienteEmail").value.trim(),
            telefono: elemento("clienteTelefono").value.trim(),
            limiteCredito: leerMonto("clienteLimite", "El limite de credito", true)
        };

        const cliente = await enviarJson(`${API_CLIENTES}/clientes`, "POST", cuerpo);

        mostrarMensaje("mensajeCrearCliente", `Cliente ${cliente.id} creado.`);
        mostrarJson("salidaCrearCliente", cliente);
        await listarClientes();
    } catch (error){
        elemento("salidaCrearCliente").textContent = "";
        mostrarMensaje("mensajeCrearCliente", error.message, true);
    }
});

//habilitacion y limite
async function cambiarHabilitacion(habilitado){
    try {
        const id = leerEntero("habClienteId", 1, "El identificador");
        const cuerpo = { habilitado, motivo: elemento("habMotivo").value.trim() };

        const cliente = await enviarJson(`${API_CLIENTES}/clientes/${id}/habilitacion`, "PUT", cuerpo);

        mostrarMensaje("mensajeHabilitacion", habilitado ? "Cliente habilitado." : "Cliente inhabilitado.");
        mostrarJson("salidaHabilitacion", cliente);
        await listarClientes();
    } catch (error){
        elemento("salidaHabilitacion").textContent = "";
        mostrarMensaje("mensajeHabilitacion", error.message, true);
    }
}

elemento("btnHabilitar").addEventListener("click", () => cambiarHabilitacion(true));
elemento("btnInhabilitar").addEventListener("click", () => cambiarHabilitacion(false));

elemento("btnCambiarLimite").addEventListener("click", async () => {
    try {
        const id = leerEntero("habClienteId", 1, "El identificador");
        const limiteCredito = leerMonto("habLimite", "El limite de credito", true);

        const cliente = await enviarJson(`${API_CLIENTES}/clientes/${id}/limite`, "PUT", { limiteCredito });

        mostrarMensaje("mensajeHabilitacion", "Limite actualizado.");
        mostrarJson("salidaHabilitacion", cliente);
        await listarClientes();
    } catch (error){
        elemento("salidaHabilitacion").textContent = "";
        mostrarMensaje("mensajeHabilitacion", error.message, true);
    }
});

//consumos, esto es lo que va a llamar ventas al armar un pedido
function leerUuidConsumo(){
    const texto = elemento("consumoUuid").value.trim();
    if(!FORMATO_UUID.test(texto)){
        throw new Error("Escribe un UUID valido o genera uno nuevo.");
    }
    return texto.toLowerCase();
}

elemento("btnNuevoUuidConsumo").addEventListener("click", () => {
    elemento("consumoUuid").value = crypto.randomUUID();
});

elemento("formConsumo").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    try {
        const id = leerEntero("consumoClienteId", 1, "El identificador");
        const uuid = leerUuidConsumo();
        const monto = leerMonto("consumoMonto", "El monto", false);

        const consumo = await enviarJson(`${API_CLIENTES}/clientes/${id}/consumos/${uuid}`, "PUT", { monto });

        mostrarMensaje("mensajeConsumo", `Consumo ${consumo.estado}.`);
        mostrarJson("salidaConsumo", consumo);
    } catch (error){
        elemento("salidaConsumo").textContent = "";
        mostrarMensaje("mensajeConsumo", error.message, true);
    }
});

elemento("btnCancelarConsumo").addEventListener("click", async () => {
    try {
        const id = leerEntero("consumoClienteId", 1, "El identificador");
        const uuid = leerUuidConsumo();

        const consumo = await solicitarJson(`${API_CLIENTES}/clientes/${id}/consumos/${uuid}/cancelacion`,
            { method: "PUT" }
        );

        mostrarMensaje("mensajeConsumo", `Consumo ${consumo.estado}.`);
        mostrarJson("salidaConsumo", consumo);
    } catch (error){
        elemento("salidaConsumo").textContent = "";
        mostrarMensaje("mensajeConsumo", error.message, true);
    }
});

elemento("consumoUuid").value = crypto.randomUUID();
listarClientes();

//impresiones
 
const API_IMPRESIONES = "http://localhost:9004/api";
 
Object.assign(ETIQUETAS, {
    documento: "Documento",
    tipo: "Tipo",
    hojasEstimadas: "Hojas estimadas",
    hojasReales: "Hojas reales",
    costo: "Costo",
    fechaCreacion: "Fecha de creacion"
});
 
function conCostoFormateado(trabajo){
    return { ...trabajo, costo: `$${Number(trabajo.costo).toFixed(2)}` };
}
 
function llenarTablaImpresiones(trabajos){
    elemento("cuerpoImpresiones").replaceChildren();
 
    for (const trabajo of trabajos){
        const fila = document.createElement("tr");
        const valores = [
            trabajo.id,
            trabajo.clienteId,
            trabajo.documento,
            trabajo.tipo,
            trabajo.hojasEstimadas,
            trabajo.hojasReales,
            `$${Number(trabajo.costo).toFixed(2)}`,
            trabajo.estado
        ];
        for (const valor of valores){
            const celda = document.createElement("td");
            celda.textContent = String(valor ?? "");
            fila.appendChild(celda);
        }
        elemento("cuerpoImpresiones").appendChild(fila);
    }
 
    elemento("tablaImpresiones").hidden = trabajos.length === 0;
}
 
async function listarImpresiones(ruta, descripcion){
    elemento("tablaImpresiones").hidden = true;
    elemento("cuerpoImpresiones").replaceChildren();
 
    mostrarMensaje("mensajeImpresiones", "consultando impresiones...");
 
    try{
        const trabajos = await solicitarJson(`${API_IMPRESIONES}/impresiones${ruta}`);
 
        if(!Array.isArray(trabajos)){
            throw new Error("El servicio no devolvio una lista.");
        }
 
        llenarTablaImpresiones(trabajos);
        mostrarMensaje("mensajeImpresiones", `${descripcion}: ${trabajos.length}.`);
    } catch (error) {
        mostrarMensaje("mensajeImpresiones", error.message, true);
    }
}
 
elemento("btnListarImpresiones").addEventListener("click", () => {
    listarImpresiones("", "Trabajos registrados");
});
 
elemento("btnColaImpresiones").addEventListener("click", () => {
    listarImpresiones("/cola", "Trabajos en cola");
});
 
//crear trabajo
elemento("formCrearImpresion").addEventListener("submit", async (evento) => {
    evento.preventDefault();
 
    try {
        const cuerpo = {
            clienteId: leerEntero("impClienteId", 1, "El cliente"),
            documento: elemento("impDocumento").value.trim(),
            tipo: elemento("impTipo").value,
            hojasEstimadas: leerEntero("impHojasEstimadas", 1, "Las hojas estimadas")
        };
 
        const trabajo = await enviarJson(`${API_IMPRESIONES}/impresiones`, "POST", cuerpo);
 
        mostrarMensaje("mensajeCrearImpresion", `Trabajo ${trabajo.id} creado.`);
        mostrarJson("salidaCrearImpresion", conCostoFormateado(trabajo));
        await listarImpresiones("/cola", "Trabajos en cola");
    } catch (error){
        elemento("salidaCrearImpresion").textContent = "";
        mostrarMensaje("mensajeCrearImpresion", error.message, true);
    }
});
 
//consultar, iniciar, terminar y cancelar
async function operarImpresion(accion, mensajeExito){
    try {
        const id = leerEntero("impTrabajoId", 1, "El identificador");
        let ruta = `${API_IMPRESIONES}/impresiones/${id}`;
        let opciones = {};
 
        if(accion === "iniciar" || accion === "cancelar"){
            ruta += `/${accion}`;
            opciones = { method: "PUT" };
        }
 
        if(accion === "terminar"){
            const hojasReales = leerEntero("impHojasReales", 1, "Las hojas reales");
            ruta += `/terminar?hojasReales=${hojasReales}`;
            opciones = { method: "PUT" };
        }
 
        const trabajo = await solicitarJson(ruta, opciones);
 
        mostrarMensaje("mensajeOperarImpresion", mensajeExito);
        mostrarJson("salidaOperarImpresion", conCostoFormateado(trabajo));
 
        if(accion !== "consultar"){
            await listarImpresiones("", "Trabajos registrados");
        }
    } catch (error){
        elemento("salidaOperarImpresion").textContent = "";
        mostrarMensaje("mensajeOperarImpresion", error.message, true);
    }
}
 
elemento("btnConsultarImpresion").addEventListener("click", () => operarImpresion("consultar", "Consulta realizada correctamente."));
elemento("btnIniciarImpresion").addEventListener("click", () => operarImpresion("iniciar", "Trabajo en impresion."));
elemento("btnTerminarImpresion").addEventListener("click", () => operarImpresion("terminar", "Trabajo terminado."));
elemento("btnCancelarImpresion").addEventListener("click", () => operarImpresion("cancelar", "Trabajo cancelado."));
 
listarImpresiones("/cola", "Trabajos en cola");
 
