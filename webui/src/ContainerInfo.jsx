import React, { Fragment } from 'react';

export class ContainerInfo extends React.Component {
	constructor(props) {
		super(props)
		this.state = {
			tempTarget: 300,
			outFlowRateMax: 999,
			injectMoleculeName: 'Water',
			injectVolume: 10,
		}
	}

	getValue(what) {
		return this.state[what]
	}

	setValue(what, value) {
		this.setState({ [what]: value })
	}

	injectStuff() {
		const { injectMoleculeName, injectVolume } = this.state
		console.log('TODO injecting', injectVolume, 'ml of', injectMoleculeName)
	}

	render() {
		const { name, maxVolume, temperature, mass, liquidVolume } = this.props.container
		const liquid = {
			'Water': 123,
			'aa/a\\a': 42,
			'Slag': 1,
		}
		const gas = {
			'aa': 42,
			'Slag': 1,
		}
		return <div className='container-info'>
			<div className='container-info-col container-props'>
				<h3 className='container-info-heading container-info-heading-primary'>
					{name}</h3>
				<dl>
					<dt>Max. Volume</dt><dd>{maxVolume}&nbsp;ml</dd>
					<dt>Temperature</dt><dd>{temperature}&nbsp;K</dd>
					<dt>Total Mass</dt><dd>{mass}&nbsp;g</dd>
					<dt>Liquid Volume</dt><dd>
						{liquidVolume}&nbsp;ml <span className='container-info-hint'>/&nbsp;{maxVolume}ml</span>
					</dd>
				</dl>
			</div>
			<div className='container-info-col container-control'>
				<h4 className='container-info-heading'>Control:</h4>
				<span className='form-row'>
					<span className='form-description'>Temperature target</span>
					<NumIn what='tempTarget' control={this} />
					<span className='unit'>K</span>
				</span>
				<span className='form-row'>
					<span className='form-description'>Out flow rate max</span>
					<NumIn what='outFlowRateMax' control={this} />
					<span className='unit'>ml/s</span>
				</span>

				<h4 className='container-info-heading'>Add:</h4>
				<span className='form-row'>
					<span className='form-description'>Molecule</span>
					<TextIn what='injectMoleculeName' control={this} />
				</span>
				<span className='form-row'>
					<span className='form-description'>Volume</span>
					<NumIn what='injectVolume' control={this} />
					<span className='unit'>ml</span>
				</span>
				<span className='form-row'><button onClick={this.injectStuff}>Add</button></span>
			</div>
			<div className='container-info-col container-composition-liquid'>
				<h4 className='container-info-heading'>Liquid composition:</h4>
				<dl>
					{Object.entries(liquid).map(([molecule, mass], i) => <Fragment key={i}>
						<dt>{molecule}</dt><dd>{mass}&nbsp;g</dd>
					</Fragment>)}
				</dl>
			</div>
			<div className='container-info-col container-composition-gas'>
				<h4 className='container-info-heading'>Gas composition:</h4>
				<dl>
					{Object.entries(gas).map(([molecule, mass], i) => <Fragment key={i}>
						<dt>{molecule}</dt><dd>{mass}&nbsp;g</dd>
					</Fragment>)}
				</dl>
			</div>
		</div>
	}
}

export default ContainerInfo;

// XXX move

const TextIn = ({ what, control, size = 10 }) => <input type='text'
	size={size}
	value={control.getValue(what)}
	onChange={e => control.setValue(what, e.target.value)}
/>

const NumIn = ({ what, control, size = 3 }) => <input type='number'
	style={{ width: `${size}em` }}
	value={control.getValue(what)}
	onChange={e => control.setValue(what, e.target.value)}
/>
