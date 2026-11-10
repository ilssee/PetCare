import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  ahoraEnLima,
  buscarPorNombre,
  campanaDisponibleAhora,
  distanciaKm,
  estaAbierta,
  estadoDeCampana,
  listarCampanas,
  listarVeterinarias,
  normalizar,
} from '../../src/modules/mapa/mapa.logica';
import { armarDetalleDeCampana } from '../../src/modules/mapa/mapa.campana';
import { Ahora, CampanaRegistro, FiltrosCampanas, FiltrosVeterinarias, VeterinariaRegistro } from '../../src/modules/mapa/mapa.types';

// Miércoles 4 de noviembre de 2026, 10:30 a. m.
const AHORA: Ahora = { fecha: '2026-11-04', dia: 'miercoles', hora: '10:30:00' };

const SIN_FILTROS_VET: FiltrosVeterinarias = { tipos: [], especies: [], servicios: [], disponibleAhora: false };
const SIN_FILTROS_CAMP: FiltrosCampanas = { estados: [], especies: [], servicios: [], costos: [], disponibleAhora: false };

function todaLaSemana(apertura: string, cierre: string) {
  return (['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo'] as const).map((dia) => ({
    dia,
    apertura,
    cierre,
  }));
}

function vet(parcial: Partial<VeterinariaRegistro> & { id: number; nombre: string }): VeterinariaRegistro {
  return {
    lat: -12.1,
    lng: -77.0,
    tipo: 'privada',
    promedio: null,
    totalResenas: 0,
    especies: ['perro'],
    servicios: [{ id: 1, nombre: 'Vacunación' }],
    horarios: todaLaSemana('08:00:00', '20:00:00'),
    ...parcial,
  };
}

function camp(parcial: Partial<CampanaRegistro> & { id: number; nombre: string }): CampanaRegistro {
  return {
    organizadorId: 100,
    organizador: 'Fundación Huellas Libres',
    descripcion: 'x',
    fechaInicio: '2026-11-01',
    fechaFin: '2026-11-10',
    horaApertura: '09:00:00',
    horaCierre: '17:00:00',
    lat: -12.06,
    lng: -77.08,
    costo: 'gratuita',
    promedio: null,
    totalResenas: 0,
    esDeVeterinaria: false,
    especies: ['perro'],
    servicios: [{ id: 1, nombre: 'Vacunación' }],
    ...parcial,
  };
}

describe('estado y disponibilidad', () => {
  it('calcula el estado de una campaña según la fecha', () => {
    assert.equal(estadoDeCampana('2026-11-05', '2026-11-10', '2026-11-04'), 'proxima');
    assert.equal(estadoDeCampana('2026-11-04', '2026-11-10', '2026-11-04'), 'activa'); // el día de inicio ya es activa
    assert.equal(estadoDeCampana('2026-11-01', '2026-11-04', '2026-11-04'), 'activa'); // el último día sigue activa
    assert.equal(estadoDeCampana('2026-11-01', '2026-11-03', '2026-11-04'), 'finalizada');
  });

  it('una veterinaria está abierta solo dentro de su franja del día', () => {
    const horarios = [{ dia: 'miercoles' as const, apertura: '09:00:00', cierre: '13:00:00' }];
    assert.equal(estaAbierta(horarios, { ...AHORA, hora: '10:30:00' }), true);
    assert.equal(estaAbierta(horarios, { ...AHORA, hora: '13:00:00' }), false); // a la hora de cierre ya cerró
    assert.equal(estaAbierta(horarios, { ...AHORA, hora: '08:59:59' }), false);
    assert.equal(estaAbierta(horarios, { ...AHORA, dia: 'jueves' }), false); // ese día no atiende
  });

  it('una campaña está disponible ahora solo si está Activa y dentro de su horario', () => {
    assert.equal(campanaDisponibleAhora(camp({ id: 1, nombre: 'a' }), AHORA), true);
    assert.equal(campanaDisponibleAhora(camp({ id: 1, nombre: 'a' }), { ...AHORA, hora: '18:00:00' }), false);
    assert.equal(
      campanaDisponibleAhora(camp({ id: 1, nombre: 'a', fechaInicio: '2026-11-06', fechaFin: '2026-11-08' }), AHORA),
      false, // Próxima: todavía no empieza
    );
  });

  it('obtiene la hora de Lima sin depender de la zona del servidor', () => {
    // 5 de noviembre a las 02:30 UTC es todavía el 4 de noviembre a las 21:30 en Lima (UTC-5).
    const ahora = ahoraEnLima(new Date('2026-11-05T02:30:00Z'));
    assert.deepEqual(ahora, { fecha: '2026-11-04', dia: 'miercoles', hora: '21:30:00' });
  });

  it('calcula la distancia entre dos puntos', () => {
    const miraflores = { lat: -12.1219, lng: -77.03 };
    const limaCentro = { lat: -12.0464, lng: -77.0428 };
    const km = distanciaKm(miraflores, limaCentro);
    assert.ok(km > 8 && km < 9.5, `distancia inesperada: ${km}`);
    assert.equal(distanciaKm(miraflores, miraflores), 0);
  });

  it('normaliza tildes y mayúsculas', () => {
    assert.equal(normalizar('  Clínica VETERINARIA '), 'clinica veterinaria');
  });
});

describe('veterinarias (RF27, RF28, RF32, RF33, RF36)', () => {
  const vets = [
    vet({ id: 1, nombre: 'Patitas Felices', tipo: 'privada', promedio: 4.5, especies: ['perro', 'gato'], lat: -12.1219, lng: -77.03 }),
    vet({ id: 2, nombre: 'Municipal Centro', tipo: 'municipal', promedio: 3, especies: ['perro', 'gato'], lat: -12.0464, lng: -77.0428, horarios: todaLaSemana('08:00:00', '16:00:00') }),
    vet({ id: 3, nombre: 'Mundo Mascota', tipo: 'privada', promedio: null, lat: -12.2, lng: -77.2, especies: ['gato', 'conejo'], servicios: [{ id: 3, nombre: 'Consulta general' }], horarios: todaLaSemana('14:00:00', '21:00:00') }),
  ];
  const opciones = { filtros: SIN_FILTROS_VET, orden: 'nombre' as const, origen: null, area: null, ahora: AHORA };

  it('sin filtros devuelve todas, ordenadas por nombre', () => {
    const r = listarVeterinarias(vets, [], opciones);
    assert.deepEqual(r.map((v) => v.nombre), ['Mundo Mascota', 'Municipal Centro', 'Patitas Felices']);
  });

  it('filtra por tipo, especie y servicio', () => {
    const municipales = listarVeterinarias(vets, [], { ...opciones, filtros: { ...SIN_FILTROS_VET, tipos: ['municipal'] } });
    assert.deepEqual(municipales.map((v) => v.id), [2]);

    const conejos = listarVeterinarias(vets, [], { ...opciones, filtros: { ...SIN_FILTROS_VET, especies: ['conejo'] } });
    assert.deepEqual(conejos.map((v) => v.id), [3]);

    const consulta = listarVeterinarias(vets, [], { ...opciones, filtros: { ...SIN_FILTROS_VET, servicios: [3] } });
    assert.deepEqual(consulta.map((v) => v.id), [3]);
  });

  it('combina filtros distintos con "y" y opciones del mismo filtro con "o"', () => {
    const r = listarVeterinarias(vets, [], {
      ...opciones,
      filtros: { ...SIN_FILTROS_VET, tipos: ['privada'], especies: ['perro', 'conejo'] },
    });
    assert.deepEqual(r.map((v) => v.id).sort(), [1, 3]);
    const ninguna = listarVeterinarias(vets, [], {
      ...opciones,
      filtros: { ...SIN_FILTROS_VET, tipos: ['municipal'], especies: ['conejo'] },
    });
    assert.deepEqual(ninguna, []);
  });

  it('"Disponible ahora" deja solo las abiertas', () => {
    // 10:30: la 3 abre a las 14:00, por eso queda fuera.
    const r = listarVeterinarias(vets, [], { ...opciones, filtros: { ...SIN_FILTROS_VET, disponibleAhora: true } });
    assert.deepEqual(r.map((v) => v.id).sort(), [1, 2]);
  });

  it('ordena por calificación (las que no tienen reseñas, al final) y por distancia', () => {
    const porCalificacion = listarVeterinarias(vets, [], { ...opciones, orden: 'calificacion' });
    assert.deepEqual(porCalificacion.map((v) => v.id), [1, 2, 3]);

    // Desde Lima Centro, la más cercana es la municipal del mismo lugar.
    const porDistancia = listarVeterinarias(vets, [], {
      ...opciones,
      orden: 'distancia',
      origen: { lat: -12.0464, lng: -77.0428 },
    });
    assert.equal(porDistancia[0].id, 2);
    assert.equal(porDistancia[0].distanciaKm, 0);
    assert.ok(porDistancia[1].distanciaKm! <= porDistancia[2].distanciaKm!);
  });

  it('solo devuelve lo que está dentro del área visible', () => {
    const area = { norte: -12.1, sur: -12.14, este: -77.0, oeste: -77.06 }; // solo Miraflores
    const r = listarVeterinarias(vets, [], { ...opciones, area });
    assert.deepEqual(r.map((v) => v.id), [1]);
  });

  it('entrega las franjas de atención de hoy y la cantidad de reseñas', () => {
    const dosFranjas = vet({
      id: 9,
      nombre: 'Con receso',
      totalResenas: 3,
      horarios: [
        { dia: 'miercoles', apertura: '15:00:00', cierre: '19:00:00' },
        { dia: 'miercoles', apertura: '09:00:00', cierre: '13:00:00' },
        { dia: 'jueves', apertura: '09:00:00', cierre: '19:00:00' },
      ],
    });
    const [r] = listarVeterinarias([dosFranjas], [], opciones);
    assert.deepEqual(r.horarioHoy, [
      { apertura: '09:00:00', cierre: '13:00:00' },
      { apertura: '15:00:00', cierre: '19:00:00' },
    ]);
    assert.equal(r.totalResenas, 3);
    const [cerrada] = listarVeterinarias([dosFranjas], [], { ...opciones, ahora: { ...AHORA, dia: 'domingo' } });
    assert.deepEqual(cerrada.horarioHoy, []);
  });

  it('marca las campañas vigentes que están en la misma ubicación de la veterinaria (RF28)', () => {
    const campanas = [
      camp({ id: 10, nombre: 'Vacunación', organizadorId: 2, esDeVeterinaria: true, lat: -12.0464, lng: -77.0428 }),
      camp({ id: 11, nombre: 'Otra sede', organizadorId: 2, esDeVeterinaria: true, lat: -12.2, lng: -77.1 }),
      camp({ id: 12, nombre: 'Finalizada', organizadorId: 2, esDeVeterinaria: true, lat: -12.0464, lng: -77.0428, fechaInicio: '2026-09-01', fechaFin: '2026-09-05' }),
    ];
    const r = listarVeterinarias(vets, campanas, opciones);
    const centro = r.find((v) => v.id === 2)!;
    assert.deepEqual(centro.campanasAqui, [{ id: 10, estado: 'activa' }]); // ni la otra sede ni la finalizada
    assert.deepEqual(r.find((v) => v.id === 1)!.campanasAqui, []);
  });
});

describe('campañas (RF27, RF28, RF34, RF35, RF36)', () => {
  const vets = [vet({ id: 2, nombre: 'Municipal Centro', lat: -12.0464, lng: -77.0428 })];
  const campanas = [
    camp({ id: 1, nombre: 'Vacunación antirrábica', organizadorId: 2, organizador: 'Municipal Centro', esDeVeterinaria: true, lat: -12.0464, lng: -77.0428, fechaInicio: '2026-11-01', fechaFin: '2026-11-08', promedio: 3 }),
    camp({ id: 2, nombre: 'Esterilización', costo: 'de_pago', fechaInicio: '2026-11-03', fechaFin: '2026-11-20', servicios: [{ id: 6, nombre: 'Esterilización' }], especies: ['perro', 'gato'], promedio: 5 }),
    camp({ id: 3, nombre: 'Microchip', costo: 'de_pago', fechaInicio: '2026-11-09', fechaFin: '2026-11-11', organizador: 'Asociación Cuatro Patas', organizadorId: 101 }),
    camp({ id: 4, nombre: 'Vieja', fechaInicio: '2026-09-01', fechaFin: '2026-09-05' }),
    camp({ id: 5, nombre: 'Solo tarde', horaApertura: '14:00:00', horaCierre: '18:00:00' }),
  ];
  const opciones = { filtros: SIN_FILTROS_CAMP, orden: 'inicio' as const, origen: null, area: null, ahora: AHORA };

  it('nunca devuelve campañas finalizadas y las ordena por fecha de inicio', () => {
    const r = listarCampanas(campanas, vets, opciones);
    // La 1 y la 5 empiezan el mismo día y se desempatan por nombre.
    assert.deepEqual(r.map((c) => c.id), [5, 1, 2, 3]);
    assert.ok(!r.some((c) => c.id === 4));
  });

  it('indica el estado de cada campaña', () => {
    const r = listarCampanas(campanas, vets, opciones);
    assert.equal(r.find((c) => c.id === 1)!.estado, 'activa');
    assert.equal(r.find((c) => c.id === 3)!.estado, 'proxima');
  });

  it('filtra por estado, especie, servicio y costo', () => {
    const proximas = listarCampanas(campanas, vets, { ...opciones, filtros: { ...SIN_FILTROS_CAMP, estados: ['proxima'] } });
    assert.deepEqual(proximas.map((c) => c.id), [3]);

    const gatos = listarCampanas(campanas, vets, { ...opciones, filtros: { ...SIN_FILTROS_CAMP, especies: ['gato'] } });
    assert.deepEqual(gatos.map((c) => c.id), [2]);

    const esteril = listarCampanas(campanas, vets, { ...opciones, filtros: { ...SIN_FILTROS_CAMP, servicios: [6] } });
    assert.deepEqual(esteril.map((c) => c.id), [2]);

    const dePago = listarCampanas(campanas, vets, { ...opciones, filtros: { ...SIN_FILTROS_CAMP, costos: ['de_pago'] } });
    assert.deepEqual(dePago.map((c) => c.id).sort(), [2, 3]);
  });

  it('"Disponible ahora" deja solo las Activas dentro de su horario de hoy', () => {
    const r = listarCampanas(campanas, vets, { ...opciones, filtros: { ...SIN_FILTROS_CAMP, disponibleAhora: true } });
    // La 5 es Activa pero atiende solo desde las 14:00; la 3 todavía no empieza.
    assert.deepEqual(r.map((c) => c.id).sort(), [1, 2]);
  });

  it('ordena por fecha de fin, por calificación del organizador y por distancia', () => {
    const porFin = listarCampanas(campanas, vets, { ...opciones, orden: 'fin' });
    assert.deepEqual(porFin.map((c) => c.id), [1, 5, 3, 2]);

    const porCalificacion = listarCampanas(campanas, vets, { ...opciones, orden: 'calificacion' });
    assert.deepEqual(porCalificacion.slice(0, 2).map((c) => c.id), [2, 1]);

    const porDistancia = listarCampanas(campanas, vets, { ...opciones, orden: 'distancia', origen: { lat: -12.0464, lng: -77.0428 } });
    assert.equal(porDistancia[0].id, 1);
  });

  it('une la campaña con su veterinaria cuando están en la misma ubicación (RF28)', () => {
    const r = listarCampanas(campanas, vets, opciones);
    const deLaVeterinaria = r.find((c) => c.id === 1)!;
    assert.equal(deLaVeterinaria.veterinariaId, 2);
    assert.equal(deLaVeterinaria.enUbicacionDeVeterinaria, true);

    const externa = r.find((c) => c.id === 2)!;
    assert.equal(externa.veterinariaId, null);
    assert.equal(externa.enUbicacionDeVeterinaria, false);
  });

  it('una campaña de una veterinaria en otra ubicación se muestra aparte', () => {
    const otraSede = camp({ id: 9, nombre: 'Otra sede', organizadorId: 2, esDeVeterinaria: true, lat: -12.2, lng: -77.1 });
    const r = listarCampanas([otraSede], vets, opciones);
    assert.equal(r[0].veterinariaId, 2);
    assert.equal(r[0].enUbicacionDeVeterinaria, false);
  });
});

describe('búsqueda por nombre (RF29)', () => {
  const vets = [
    vet({ id: 1, nombre: 'Clínica Veterinaria Patitas Felices' }),
    vet({ id: 2, nombre: 'Municipal Centro', lat: -12.0464, lng: -77.0428 }),
  ];
  const campanas = [
    camp({ id: 1, nombre: 'Vacunación antirrábica', organizadorId: 2, organizador: 'Municipal Centro', esDeVeterinaria: true }),
    camp({ id: 2, nombre: 'Esterilización a bajo costo', organizadorId: 100, organizador: 'Fundación Huellas Libres' }),
    camp({ id: 3, nombre: 'Jornada de vacunación', organizadorId: 100, organizador: 'Fundación Huellas Libres' }),
    camp({ id: 4, nombre: 'Vacunación vieja', organizadorId: 100, fechaInicio: '2026-09-01', fechaFin: '2026-09-05' }),
  ];
  const o = { origen: null, area: null, ahora: AHORA };
  const etiquetas = (r: ReturnType<typeof buscarPorNombre>) => r.map((x) => `${x.clase}:${x.id}`);

  it('encuentra una veterinaria por su nombre, sin importar tildes ni mayúsculas', () => {
    assert.deepEqual(etiquetas(buscarPorNombre('CLINICA veterinaria', vets, campanas, o)), ['veterinaria:1']);
  });

  it('si coincide una campaña de una veterinaria, muestra la veterinaria y no la campaña', () => {
    assert.deepEqual(etiquetas(buscarPorNombre('antirrabica', vets, campanas, o)), ['veterinaria:2']);
  });

  it('no repite la veterinaria si coinciden ella y su campaña', () => {
    const r = buscarPorNombre('municipal', vets, campanas, o);
    assert.deepEqual(etiquetas(r), ['veterinaria:2']);
  });

  it('muestra las campañas de una entidad sin veterinaria, por su nombre o el de la entidad', () => {
    assert.deepEqual(etiquetas(buscarPorNombre('esterilizacion', vets, campanas, o)), ['campana:2']);
    assert.deepEqual(etiquetas(buscarPorNombre('huellas libres', vets, campanas, o)), ['campana:2', 'campana:3']);
  });

  it('no incluye campañas finalizadas', () => {
    const r = buscarPorNombre('vacunación', vets, campanas, o);
    assert.ok(!r.some((x) => x.clase === 'campana' && x.id === 4));
  });

  it('devuelve una lista vacía si no hay coincidencias', () => {
    assert.deepEqual(buscarPorNombre('zzz', vets, campanas, o), []);
  });

  it('ordena por distancia cuando se conoce la ubicación', () => {
    const r = buscarPorNombre('a', vets, campanas, { ...o, origen: { lat: -12.0464, lng: -77.0428 } });
    const distancias = r.map((x) => x.distanciaKm!);
    assert.deepEqual([...distancias].sort((a, b) => a - b), distancias);
  });
});

describe('detalle de una campaña (RF43)', () => {
  it('una campaña de una entidad externa trae su descripción, dirección y el resumen del organizador', () => {
    const c = camp({ id: 1, nombre: 'Feria', descripcion: 'Adopciones', promedio: 4.5, totalResenas: 12 });
    const d = armarDetalleDeCampana(c, [], AHORA, null, 'Plaza Bolognesi, Breña');
    assert.equal(d.descripcion, 'Adopciones');
    assert.equal(d.direccion, 'Plaza Bolognesi, Breña');
    assert.equal(d.veterinariaId, null);
    assert.deepEqual(d.organizadorDetalle, { id: 100, nombre: 'Fundación Huellas Libres', promedio: 4.5, totalResenas: 12 });
  });

  it('una campaña de una veterinaria indica cuál es, para abrir su perfil', () => {
    const v = vet({ id: 7, nombre: 'Clínica' });
    const c = camp({ id: 2, nombre: 'Vacunación', organizadorId: 7, organizador: 'Clínica', esDeVeterinaria: true });
    const d = armarDetalleDeCampana(c, [v], AHORA, null, null);
    assert.equal(d.veterinariaId, 7);
    assert.equal(d.direccion, null);
    assert.equal(d.organizadorDetalle.promedio, null);
  });

  it('calcula el estado y la distancia como en el mapa', () => {
    const c = camp({ id: 3, nombre: 'Campaña', fechaInicio: '2026-11-08', fechaFin: '2026-11-09' });
    const d = armarDetalleDeCampana(c, [], AHORA, { lat: -12.06, lng: -77.08 }, null);
    assert.equal(d.estado, 'proxima');
    assert.equal(d.distanciaKm, 0);
  });
});
