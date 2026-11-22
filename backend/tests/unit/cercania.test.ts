import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { avisosDeLugar } from '../../src/modules/cercania/cercania.logica';
import { avisarLugaresNuevos, guardar, MENSAJE_SIN_UBICACION } from '../../src/modules/cercania/cercania.service';
import { LugarNuevo, UsuarioCercano } from '../../src/modules/cercania/cercania.types';

// Usuario en Miraflores con un radio de 3 km.
const MIRAFLORES = { lat: -12.1219, lng: -77.03 };

function usuario(parcial: Partial<UsuarioCercano> = {}): UsuarioCercano {
  return { usuarioId: 1, ...MIRAFLORES, radioKm: 3, notificarCampanas: true, notificarVeterinarias: true, ...parcial };
}

const campanaCerca: LugarNuevo = {
  tipo: 'campana',
  id: 10,
  nombre: 'Vacunación antirrábica gratuita',
  organizador: 'Centro Veterinario Municipal Norte',
  lat: -12.1,
  lng: -77.03, // a unos 2.4 km
};
const campanaLejos: LugarNuevo = { ...campanaCerca, id: 11, lat: -12.0464, lng: -77.0428 }; // a más de 8 km
const veterinariaCerca: LugarNuevo = {
  tipo: 'veterinaria',
  id: 20,
  nombre: 'Huellitas Pet Salud',
  organizador: 'Huellitas Pet Salud',
  lat: -12.1,
  lng: -77.03,
};

describe('a quién se avisa (RF39 y RF40)', () => {
  it('avisa de una campaña nueva dentro del radio', () => {
    const [aviso] = avisosDeLugar(campanaCerca, [usuario()]);
    assert.equal(aviso.usuarioId, 1);
    assert.equal(aviso.tipo, 'campana_cercana');
    assert.equal(aviso.titulo, 'Nueva campaña cerca de ti');
    assert.match(aviso.cuerpo, /Vacunación antirrábica gratuita \(Centro Veterinario Municipal Norte\), a 2\.4 km de tu ubicación\./);
  });

  it('avisa de una veterinaria nueva dentro del radio', () => {
    const [aviso] = avisosDeLugar(veterinariaCerca, [usuario()]);
    assert.equal(aviso.tipo, 'veterinaria_cercana');
    assert.equal(aviso.titulo, 'Nueva veterinaria cerca de ti');
    assert.match(aviso.cuerpo, /^Huellitas Pet Salud, a 2\.4 km de tu ubicación\.$/);
  });

  it('el aviso lleva la variante del lugar para pintar el ícono como en el mapa', () => {
    const [deCampana] = avisosDeLugar({ ...campanaCerca, variante: 'proxima' }, [usuario()]);
    assert.equal(deCampana.variante, 'proxima');
    const [deVeterinaria] = avisosDeLugar({ ...veterinariaCerca, variante: 'municipal' }, [usuario()]);
    assert.equal(deVeterinaria.variante, 'municipal');
    const [sinVariante] = avisosDeLugar(veterinariaCerca, [usuario()]);
    assert.equal(sinVariante.variante, undefined);
  });

  it('el aviso dice qué pantalla abrir al tocarlo', () => {
    // Veterinaria nueva: su perfil.
    const [deVeterinaria] = avisosDeLugar(veterinariaCerca, [usuario()]);
    assert.equal(deVeterinaria.lugarId, 20);
    assert.equal(deVeterinaria.veterinariaId, 20);
    // Campaña de una veterinaria: el perfil de esa veterinaria, con la campaña.
    const [deVeterinariaConCampana] = avisosDeLugar({ ...campanaCerca, veterinariaId: 2 }, [usuario()]);
    assert.equal(deVeterinariaConCampana.lugarId, 10);
    assert.equal(deVeterinariaConCampana.veterinariaId, 2);
    // Campaña de una entidad externa: solo la campaña (sin perfil de veterinaria).
    const [externa] = avisosDeLugar(campanaCerca, [usuario()]);
    assert.equal(externa.lugarId, 10);
    assert.equal(externa.veterinariaId, undefined);
  });

  it('no avisa si el lugar queda fuera del radio', () => {
    assert.deepEqual(avisosDeLugar(campanaLejos, [usuario()]), []);
  });

  it('avisa si la distancia es igual al radio, y no si lo pasa por poco', () => {
    const exacto = usuario({ radioKm: 2.4 });
    // 2.4 km redondeado; la distancia real es un poco mayor, así que con 2.4 no entra y con 2.5 sí.
    assert.deepEqual(avisosDeLugar(campanaCerca, [exacto]), []);
    assert.equal(avisosDeLugar(campanaCerca, [usuario({ radioKm: 2.5 })]).length, 1);
  });

  it('respeta cada tipo de aviso por separado', () => {
    const soloCampanas = usuario({ notificarVeterinarias: false });
    assert.equal(avisosDeLugar(campanaCerca, [soloCampanas]).length, 1);
    assert.equal(avisosDeLugar(veterinariaCerca, [soloCampanas]).length, 0);

    const soloVeterinarias = usuario({ notificarCampanas: false });
    assert.equal(avisosDeLugar(campanaCerca, [soloVeterinarias]).length, 0);
    assert.equal(avisosDeLugar(veterinariaCerca, [soloVeterinarias]).length, 1);
  });

  it('avisa solo a los usuarios que lo tienen cerca', () => {
    const lejano = usuario({ usuarioId: 2, lat: -12.0464, lng: -77.0428 });
    const avisos = avisosDeLugar(campanaCerca, [usuario({ usuarioId: 1 }), lejano, usuario({ usuarioId: 3 })]);
    assert.deepEqual(avisos.map((a) => a.usuarioId), [1, 3]);
  });
});

describe('configuración de ubicación y radio (RF37 y RF38)', () => {
  // Estas validaciones fallan antes de tocar la base de datos, así que se pueden probar sin ella.
  it('no deja activar un aviso sin ubicación y radio', async () => {
    await assert.rejects(guardar(1, { notificarCampanas: true }), { message: MENSAJE_SIN_UBICACION });
    await assert.rejects(guardar(1, { lat: -12.1, lng: -77.03, notificarVeterinarias: true }), { message: MENSAJE_SIN_UBICACION });
    await assert.rejects(guardar(1, { radioKm: 3, notificarCampanas: true }), { message: MENSAJE_SIN_UBICACION });
  });

  it('exige la latitud y la longitud juntas', async () => {
    await assert.rejects(guardar(1, { lat: -12.1, radioKm: 3 }), { message: 'Envía la latitud y la longitud juntas' });
  });

  it('rechaza valores fuera de rango o que no son números', async () => {
    await assert.rejects(guardar(1, { lat: 120, lng: -77 }), { message: 'La latitud no es válida' });
    await assert.rejects(guardar(1, { lat: -12, lng: -200 }), { message: 'La longitud no es válida' });
    await assert.rejects(guardar(1, { lat: -12, lng: -77, radioKm: 0 }), { message: 'El radio no es válido' });
    await assert.rejects(guardar(1, { lat: -12, lng: -77, radioKm: 500 }), { message: 'El radio no es válido' });
    await assert.rejects(guardar(1, { lat: -12, lng: -77, radioKm: 'abc' }), { message: 'El radio no es válido' });
    await assert.rejects(guardar(1, { notificarCampanas: 'si' }), { message: 'El aviso de campañas debe ser verdadero o falso' });
  });
});

describe('envío de los avisos de lugares nuevos', () => {
  it('exporta la función que usa el proceso programado', () => {
    assert.equal(typeof avisarLugaresNuevos, 'function');
  });
});
