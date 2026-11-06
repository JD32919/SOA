"use strict";

const API_CATALOGO = "http://localhost:9001/api";

const elemento = (id) => document.getElementById(id);

const ETIQUETAS ={
    id: "ID",
    nombre: "Nombre",
    precio: "Precio",
    existencias: "Existencias",
    activo: "Activo";
    operacionId: "UUID de la operacion",
    estado: "Estado",
    lineas: "Lineas reservadas"
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

        } else if (clave === "precio") {
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
            : `El servicio respondio con HTTP $(respuesta.status}.`;

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